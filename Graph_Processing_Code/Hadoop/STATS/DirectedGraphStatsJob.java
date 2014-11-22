package org.hadoop.test.jobs.tasks.stats;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.jobcontrol.Job;
import org.apache.hadoop.util.Tool;
import org.hadoop.test.combine.StatsCCCombiner;
import org.hadoop.test.data.util.StatsCCContainer;
import org.hadoop.test.map.directed.DirectedStatsCCMap;
import org.hadoop.test.reduce.directed.DirectedStatsCCReducer;
import org.hadoop.test.utils.directed.DirectedNodeNeighbourTextInputFormat;

import java.io.IOException;

public class DirectedGraphStatsJob extends Configured implements Tool {
    public int run(String[] args) throws IOException {

        long t0 = System.currentTimeMillis();

        JobConf calculateCCConf = new JobConf(new Configuration());
        Job job3 = new Job(calculateCCConf);
        calculateCCConf.setJarByClass(DirectedGraphStatsJob.class);

        calculateCCConf.setMapOutputKeyClass(IntWritable.class);
        calculateCCConf.setMapOutputValueClass(StatsCCContainer.class);

        calculateCCConf.setMapperClass(DirectedStatsCCMap.class);
        calculateCCConf.setCombinerClass(StatsCCCombiner.class);
        calculateCCConf.setReducerClass(DirectedStatsCCReducer.class);

        calculateCCConf.setOutputKeyClass(NullWritable.class);
        calculateCCConf.setOutputValueClass(StatsCCContainer.class);

        calculateCCConf.setInputFormat(DirectedNodeNeighbourTextInputFormat.class);
        calculateCCConf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.addInputPath(calculateCCConf, new Path(args[5] + "/nodeNeighbourhood"));
        FileOutputFormat.setOutputPath(calculateCCConf, new Path(args[5] + "/cc_stats"));

        calculateCCConf.setNumMapTasks(Integer.parseInt(args[2]) + (Integer.parseInt(args[3]) - 1));
        calculateCCConf.setNumReduceTasks(1);

        //platform config
        /* Comment to to perform test in local-mode */

        /* pseudo */
        //calculateCCConf.set("io.sort.mb", "768");
        //calculateCCConf.set("fs.inmemory.size.mb", "768");
        //calculateCCConf.set("io.sort.factor", "50");

        /* DAS4 conf Hadoop ver 0.20.203 */
        calculateCCConf.set("io.sort.mb", "1536");
        calculateCCConf.set("io.sort.factor", "80");
        calculateCCConf.set("fs.inmemory.size.mb", "1536");

        JobClient.runJob(calculateCCConf);

        // recording end time of stats
        long t1 = System.currentTimeMillis();

        double elapsedStatsTimeSeconds = (t1 - t0)/1000.0;
        System.out.println("Stats_Texe = "+elapsedStatsTimeSeconds);

        if(Boolean.parseBoolean(args[1])) {
            System.out.println("\n@@@ Deleting intermediate results");
            FileSystem dfs = FileSystem.get(calculateCCConf);
            dfs.delete(new Path(args[5]+"/nodeNeighbourhood"), true);
        }

        System.out.println("\n***********************************************************");
        System.out.println("* basic stats and average clustering coefficient FINISHED *");
        System.out.println("***********************************************************\n");

        // creating benchmark data
        double elapsedTimeSeconds = (t1 - Long.parseLong(args[0]))/1000.0;
        System.out.println("Texe = "+elapsedTimeSeconds);
        try{
            FileSystem fs = FileSystem.get(calculateCCConf);
            Path path = new Path(args[5]+"/benchmark.txt");
            FSDataOutputStream os = fs.create(path);
            String benchmarkData = "elapsed time: "+elapsedTimeSeconds;
            os.write(benchmarkData.getBytes());
            os.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return 0;
    }
}