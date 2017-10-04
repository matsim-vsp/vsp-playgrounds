/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.opdyts.equil;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import com.jcraft.jsch.*;
import playground.agarwalamit.parametricRuns.JobScriptWriter;

/**
 * A class to create a job script, write it on remote and then run the job based on the given parameters.
 *
 * Created by amit on 04.10.17.
 */

public class ParametricRunsEquilnet {

    private static final String newLine = System.getProperty("line.separator");
    private final Session session;
    private final ChannelSftp sftp;

    private int jobCounter ;

    public ParametricRunsEquilnet(int jobStartCounter) {
        this.jobCounter = jobStartCounter;
        try {
            JSch jSch = new JSch();
            jSch.setKnownHosts("~/.ssh/known_hosts"); // location of the ssh fingerprint (unique host key)
            jSch.addIdentity("~/.ssh/id_rsa_tub_math"); // this is the private key required.

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // so that no question asked, and script run without any problem

            session = jSch.getSession("agarwal", "cluster-i.math.tu-berlin.de", 22);
            session.setConfig(config);

            session.connect();

            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
        } catch (JSchException e) {
            throw new RuntimeException("Aborting. Reason : " + e);
        }
    }

    public static void main(String[] args) {
        String baseDir = "/net/ils4/agarwal/equilOpdyts/carBicycle/output/";
        StringBuilder buffer = new StringBuilder();
        ParametricRunsEquilnet parametricRuns = new ParametricRunsEquilnet(1);

        String ascStyles [] = {"axial_fixed","axial_random"};
        double [] stepSizes = {0.25, 0.5, 0.75, 1.0};
        Integer [] convIterations = {500, 300};
        double [] selfTuningWts = {1.0, 2.0};
        Integer [] warmUpIts = {1,5,10};

        buffer.append("runNr\tascStyle\tstepSize\titerations2Convergence\tselfTunerWt\twarmUpIts"+newLine);

        for (String ascStyle : ascStyles ) {
            for(double stepSize :stepSizes){
                for (int conIts : convIterations) {
                    for (double selfTunWt : selfTuningWts) {
                        for (int warmUpIt : warmUpIts) {
                            String arg = ascStyle + " "+ stepSize + " " + conIts + " " + selfTunWt + " " + warmUpIt;
                            parametricRuns.run(arg, baseDir);
                            buffer.append(arg+newLine);
                        }
                    }
                }
            }
        }

        parametricRuns.writeRemoteFile(buffer, baseDir+"/runInfo.txt");
        parametricRuns.close();
    }

    public void writeRemoteFile(final StringBuilder buffer, final String file) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream w = new DataOutputStream(baos);
            w.writeBytes(buffer.toString());
            w.flush();

            sftp.put(new ByteArrayInputStream(baos.toByteArray()), file);

            w.close();
            baos.close();
        } catch (SftpException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    public void close(){
        session.disconnect();
        sftp.disconnect();
    }

    public void run(final String argument, final String baseDir) {
        String [] commands = prepareCommands(argument, baseDir);

        StringBuilder output = new StringBuilder();
        Arrays.stream(commands).forEach(cmd -> executeCommand(cmd, output));

        System.out.println(output.toString());
    }

    private void executeCommand( final String command, final StringBuilder output) {
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");

            channel.setCommand(command);
            InputStream stdout = channel.getInputStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stdout));
            channel.connect();
            String line = bufferedReader.readLine();
            while ( line != null) {
                output.append(line+"\n");
                line = bufferedReader.readLine();
            }
            channel.disconnect();
        } catch (JSchException e) {
            throw new RuntimeException("Aborting. Reason : " + e);
        } catch (IOException exx) {
            throw new RuntimeException("Data is not written/read. Reason : " + exx);
        }
    }

    private String [] prepareCommands(final String argument, final String baseDir){

        String jobName = "run"+String.valueOf(jobCounter++);
        String locationOfOutput = baseDir.endsWith("/") ? baseDir: baseDir+"/" +jobName+"/";

        // create dir: if dir exits, an exception will be thrown.
        boolean isExists = false;
        try {
            sftp.lstat(locationOfOutput);
            isExists = true;
        } catch (SftpException e) {
            isExists = false;
        }

        if (! isExists) {
            try {
                sftp.mkdir(locationOfOutput);
            } catch (SftpException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
        }

        // location of file must be locale and then can be copied to remote.
        String jobScriptFileName = locationOfOutput+"/testScriptCommandLine.sh";

        String matsimDir = "r_0d0d339d49c0a6ef1d8ec835f10924a92107b2c1_opdyts";


        String [] additionalLines = {
                "echo \"========================\"",
                "echo \" "+matsimDir+" \" ",
                "echo \"========================\"",
                newLine,

                "cd /net/ils4/agarwal/matsim/"+matsimDir+"/",
                newLine,

                "java -Djava.awt.headless=true -Xmx29G -cp agarwalamit-0.10.0-SNAPSHOT.jar " +
                        "playground/agarwalamit/opdyts/equil/MatsimOpdytsEquilMixedTrafficIntegration " +
                        "/net/ils4/agarwal/equilOpdyts/carBicycle/inputs/ " +
                        "/net/ils4/agarwal/equilOpdyts/carBicycle/output/"+jobName+"/ " +
                        "/net/ils4/agarwal/equilOpdyts/carBicycle/relaxedPlans/output_plans.xml.gz "+
                        argument+" "
        };

        JobScriptWriter scriptWriter = new JobScriptWriter();
        scriptWriter.appendCommands( jobName, locationOfOutput, additionalLines);
        scriptWriter.writeRemoteLocation(sftp, jobScriptFileName);

        return new String [] {
                "qstat -u agarwal",
                "qsub "+scriptWriter.getJobScript(),
                "qstat -u agarwal" };
    }
}
