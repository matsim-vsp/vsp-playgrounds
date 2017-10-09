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

package playground.agarwalamit.templates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;

import com.google.common.base.Charsets;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
//import playground.sebhoerl.mexec.ssh.utils.SSHFile;
//import playground.sebhoerl.mexec.ssh.utils.SSHUtils;

/**
 * Created by amit on 28/03/2017.
 */

public class RunSSHScript {

    // create a private key as follows:
    // ssh-keygen -t rsa -b 4096 -f $HOME/.ssh/id_rsa_hlrn
    // see brief documentation https://www.hlrn.de/home/view/System3/PubkeyLogin
    // afterward, password is not required.
//    private static final String myPassword = "xxx";

    public static void main(String[] args) {
        new RunSSHScript().runViaChannelDirectly();
//        new RunSSHScript().runViaSSHUtils();
    }

    /*
     * dependency only on Jsch lib.
     */
    public void runViaChannelDirectly() {

        try {
            JSch jSch = new JSch();
            jSch.setKnownHosts("~/.ssh/known_hosts"); // location of the ssh fingerprint (unique host key)
            jSch.addIdentity("~/.ssh/id_rsa_tub_math"); // this is the private key required.


            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // so that no question asked, and script run without any problem

            Session session = jSch.getSession("agarwal", "cluster-i.math.tu-berlin.de", 22);
            session.setConfig(config);
//            session.setPassword(myPassword);

            try {
                session.connect();

                String script = "/net/ils4/agarwal/patnaOpdyts/script_allModes.sh";

                String myJobs = "qstat -u agarwal";
                String [] commands = {
                        myJobs,
                        "ls -l /net/ils4/agarwal/patnaOpdyts/output_allModes/stateVectorFiles/*/*",
//                      "qsub "+script,
                        myJobs
                };

                StringBuilder output = new StringBuilder();
                Arrays.stream(commands).forEach(cmd -> executeCommand(session, cmd, output));

                System.out.println(output.toString());
            } finally {
                session.disconnect();
            }
        } catch (JSchException e) {
            throw new RuntimeException("Aborting. Reason : " + e);
        }
    }

    private void executeCommand(final Session session, final String command, final StringBuilder output) {
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

    public void runViaSSHUtils() {
        try {
            JSch jSch = new JSch();
            jSch.setKnownHosts("~/.ssh/known_hosts"); // location of the ssh fingerprint (unique host key)

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // so that no question asked, and script run without any problem

            Session session = jSch.getSession("agarwal", "cluster-i.math.tu-berlin.de", 22);
            session.setConfig(config);
//            session.setPassword(myPassword);

            try {
                session.connect();
//FIXME: sebhoerl playground is no longer available
                //
//                SSHUtils sshUtils = new SSHUtils(session);
//                String script = "/net/ils4/agarwal/patnaOpdyts/script_allModes.sh";
//
//                sshUtils.mkdirs(new SSHFile("/net/ils4/agarwal/testDir/"));
//
//                SSHUtils.RunResult runResult = sshUtils.execute("qstat -u agarwal");
//                System.out.println(runResult.output); // TODO idk, how to see the output on the console.
//
//                sshUtils.execute("qsub "+script);
            } finally {
                session.disconnect();
            }
        } catch (JSchException e) {
            throw new RuntimeException("Aborting. Reason : " + e);
//        } catch (IOException exx) {
//            throw new RuntimeException("Data is not written/read. Reason : " + exx);
        }
    }
}
