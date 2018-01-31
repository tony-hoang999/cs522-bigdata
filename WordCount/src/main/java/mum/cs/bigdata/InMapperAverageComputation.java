package mum.cs.bigdata;

import java.io.IOException;

import mum.cs.bigdata.utils.IntPairWritable;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;

public class InMapperAverageComputation extends Configured implements Tool {
	private static final Logger logger = Logger.getLogger(WordCount.class);
	
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new InMapperAverageComputation(), args);
		System.exit(res);
	}

	public int run(String[] args) throws Exception {
		Job job = Job.getInstance(getConf(), "wordcount");
		job.setJarByClass(this.getClass());
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.setMapperClass(Map.class);
		job.setCombinerClass(Combine.class);
		job.setReducerClass(Reduce.class);
		job.setOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntPairWritable.class);
		job.setOutputValueClass(DoubleWritable.class);
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	public static class Map extends Mapper<LongWritable, Text, Text, IntPairWritable> {
	    private Text word = new Text();
	    
	    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
	        String line = value.toString();
	        String[] strs = line.split(" ");
	        
	        if (strs.length > 1) {
	        	String ipString = strs[0];
	        	String capacity = strs[strs.length-1];
	        	
	        	logger.info("InMapperAverageComputation.Mapper");
	        	
	        	try {
	        		int number = Integer.parseInt(capacity);
		        	word.set(ipString);
		        	IntPairWritable pair = new IntPairWritable(number, 1);
		        	logger.info("\n<" + word + ", ( " + pair.getFirst() + ", " + pair.getSecond() + ")>");
		        	context.write(word, pair);
	        	} catch(NumberFormatException ex) {
	        		
	        	}
	        }
	    }
	}

	public static class Combine extends Reducer<Text, IntPairWritable, Text, IntPairWritable> {
    	public void reduce(Text word, Iterable<IntPairWritable> pairs, Context context) throws IOException, InterruptedException {
    		int sum = 0;
    		int count = 0;
    		
    		for (IntPairWritable pair : pairs) {
    			sum += pair.getFirst();
    			count += pair.getSecond();
    		}
    		
    		context.write(word, new IntPairWritable(sum, count));
    	}
    }
	
	public static class Reduce extends Reducer<Text, IntPairWritable, Text, DoubleWritable> {
		@Override
		public void reduce(Text word, Iterable<IntPairWritable> pairs, Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			int cnt = 0;
			
			logger.info("InMapperAverageComputation.Reducer");
			
			for (IntPairWritable pair : pairs) {
				logger.info("\n<" + word + ", ( " + pair.getFirst() + ", " + pair.getSecond() + ")>");
				sum += pair.getFirst();
				cnt += pair.getSecond();
			}
			context.write(word, new DoubleWritable(sum/(cnt*1.0)));
		}
	}
}