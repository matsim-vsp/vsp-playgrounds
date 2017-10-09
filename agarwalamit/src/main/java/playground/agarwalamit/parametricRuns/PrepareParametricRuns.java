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

package playground.agarwalamit.parametricRuns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;
import com.google.common.base.Charsets;
import com.jcraft.jsch.*;

/**
 * A class to create a job script, write it on remote and then run the job based on the given parameters.
 *
 * Created by amit on 04.10.17.
 */

public class PrepareParametricRuns {

    private static final String newLine = System.getProperty("line.separator");
    private final Session session;
    private final ChannelSftp sftp;

    private int jobCounter ;

    public PrepareParametricRuns (int jobStartCounter) {
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

    // An example
    public static void main(String[] args) {
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns(11);
        String ascStyles [] = {"axial_fixed","axial_random"};
        for (String arg : ascStyles ) {
            parametricRuns.run(arg);
        }
        parametricRuns.close();
    }

    public void close(){
        session.disconnect();
        sftp.disconnect();
    }

    public void run(String argument) {
        String [] commands = prepareCommands(argument);

        StringBuilder output = new StringBuilder();
        Arrays.stream(commands).forEach(cmd -> executeCommand(cmd, output));

        System.out.println(output.toString());
    }

    private void executeCommand( final String command, final StringBuilder output) {
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");

            channel.setCommand(command);
            InputStream stdout = channel.getInputStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stdout, Charsets.UTF_8));
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

    private String [] prepareCommands(String argument){
        String baseDir = "/net/ils4/agarwal/equilOpdyts/carBicycle/";
        String jobName = "run"+String.valueOf(jobCounter++);
        String locationOfOutput = baseDir+"/output/"+jobName+"/";

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

        String [] additionalLines = {
                "echo \"========================\"",
                "echo \"r_6fcba9f631fedc82ecc01a48bbc43abfefac78c1_opdyts\"",
                "echo \"========================\"",
                newLine,

                "cd /net/ils4/agarwal/matsim/r_6fcba9f631fedc82ecc01a48bbc43abfefac78c1_opdyts/",
                newLine,

                "java -Djava.awt.headless=true -Xmx29G -cp agarwalamit-0.10.0-SNAPSHOT.jar " +
                        "playground/agarwalamit/opdyts/equil/MatsimOpdytsEquilMixedTrafficIntegration " +
                        "/net/ils4/agarwal/equilOpdyts/carBicycle/inputs/ " +
                        "/net/ils4/agarwal/equilOpdyts/carBicycle/output/"+jobName+"/ " +
                        "/net/ils4/agarwal/equilOpdyts/carBicycle/relaxedPlans/output_plans.xml.gz "+
                        argument
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
