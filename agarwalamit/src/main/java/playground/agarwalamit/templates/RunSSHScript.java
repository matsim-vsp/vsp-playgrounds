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
import java.util.Properties;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import playground.sebhoerl.mexec.ssh.utils.SSHFile;
import playground.sebhoerl.mexec.ssh.utils.SSHUtils;

/**
 * Created by amit on 28/03/2017.
 */

public class RunSSHScript {

    private static final String myPassword = "xxxx";

    public static void main(String[] args) {
        new RunSSHScript().runViaChannelDirectly();
    }

    /*
     * dependency only on Jsch lib.
     */
    public static void runViaChannelDirectly() {

        try {
            JSch jSch = new JSch();
            jSch.setKnownHosts("~/.ssh/known_hosts"); // location of the ssh fingerprint (unique host key)

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // so that no question asked, and script run without any problem

            Session session = jSch.getSession("agarwal", "cluster-i.math.tu-berlin.de", 22);
            session.setConfig(config);
            session.setPassword(myPassword);

            try {
                session.connect();

                ChannelExec channel = (ChannelExec) session.openChannel("exec");

                channel.setCommand("ls -l");

                String script = "/net/ils4/agarwal/patnaOpdyts/script_allModes.sh";
//                channel.setCommand("qsub "+script);

                String myJobs = "qstat -u agarwal";
                channel.setCommand(myJobs);

                StringBuilder output = new StringBuilder();
                InputStream stdout = channel.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stdout));

                channel.connect();
                String line = bufferedReader.readLine();
                while ( line != null) {
                    output.append(line+"\n");
                    line = bufferedReader.readLine();
                }

                System.out.println(output.toString());
                channel.disconnect();
            } finally {
                session.disconnect();
            }
        } catch (JSchException e) {
            throw new RuntimeException("Aborting. Reason : " + e);
        } catch (IOException exx) {
            throw new RuntimeException("Data is not written/read. Reason : " + exx);
        }
    }

    public static void runViaSSHUtils() {
        try {
            JSch jSch = new JSch();
            jSch.setKnownHosts("~/.ssh/known_hosts"); // location of the ssh fingerprint (unique host key)

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // so that no question asked, and script run without any problem

            Session session = jSch.getSession("agarwal", "cluster-i.math.tu-berlin.de", 22);
            session.setConfig(config);
            session.setPassword(myPassword);

            try {
                session.connect();

                SSHUtils sshUtils = new SSHUtils(session);
                String script = "/net/ils4/agarwal/patnaOpdyts/script_allModes.sh";

                sshUtils.mkdirs(new SSHFile("/net/ils4/agarwal/testDir/"));

                SSHUtils.RunResult runResult = sshUtils.execute("qstat -u agarwal");
                System.out.println(runResult.output); // TODO idk, how to see the output on the console.

                sshUtils.execute("qsub "+script);
            } finally {
                session.disconnect();
            }
        } catch (JSchException e) {
            throw new RuntimeException("Aborting. Reason : " + e);
        } catch (IOException exx) {
            throw new RuntimeException("Data is not written/read. Reason : " + exx);
        }
    }
}
