/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
package com.igeekinc.indelible.indeliblefs.fusefs.linux;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.igeekinc.indelible.indeliblefs.IndelibleDirectoryNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFSServer;
import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileLike;
import com.igeekinc.indelible.indeliblefs.IndelibleServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverReceiver;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSession;
import com.igeekinc.indelible.indeliblefs.datamover.DataMoverSource;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.firehose.IndelibleFSClient;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSDirHandle;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSFUSEVolume;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSFileHandle;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSHandleManager;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSInode;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSInodeManager;
import com.igeekinc.indelible.indeliblefs.security.AuthenticationFailureException;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthentication;
import com.igeekinc.indelible.indeliblefs.security.EntityAuthenticationClient;
import com.igeekinc.indelible.oid.GeneratorID;
import com.igeekinc.indelible.oid.GeneratorIDFactory;
import com.igeekinc.indelible.oid.ObjectIDFactory;
import com.igeekinc.indelible.server.IndelibleServerPreferences;
import com.igeekinc.luwak.FUSEChannel;
import com.igeekinc.luwak.FUSEDispatch;
import com.igeekinc.luwak.inode.FUSEInodeAdapter;
import com.igeekinc.luwak.linux.LinuxFUSEDispatch;
import com.igeekinc.luwak.linux.LinuxLibC;
import com.igeekinc.luwak.lowlevel.FUSELowLevel;
import com.igeekinc.util.ClientFile;
import com.igeekinc.util.MonitoredProperties;
import com.igeekinc.util.SystemInfo;
import com.igeekinc.util.linux.LinuxFileMetaData;
import com.igeekinc.util.linux.LinuxFileMetaDataProperties;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;


public class IndelibleFSFuseMain
{
	FUSELowLevel lowLevelFS;
	FUSEChannel channel;
	FUSEDispatch dispatch;


	public IndelibleFSFuseMain()
	{
	}
	
	
	public void setupMount() throws IOException, InterruptedException
	{
		String devName = "/dev/fuse";
		
		channel = new FUSEChannel(new File(devName));
		
		String opts = "fd="+channel.getFDNum()+",rootmode=40000,user_id=0,group_id=0";
		byte [] optsBytes = opts.getBytes(Charset.forName("UTF-8"));
		Memory optsBuf = new Memory(optsBytes.length + 1);
		optsBuf.write(0, optsBytes, 0, optsBytes.length);
		optsBuf.setByte(optsBytes.length, (byte) 0);	// NULL-terminate!
		if (LinuxLibC.INSTANCE.mount("fuse", "/tmp/aa", "fuse", new NativeLong(6), optsBuf) != 0)
		{
			throw new IOException("mount failed with error code = "+Native.getLastError());
		}
	}
	
	public void startLoop()
	{
		FUSEDispatch dispatch = new LinuxFUSEDispatch(channel, lowLevelFS);
		dispatch.start();
	}
    
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
    public static void main(String [] args) throws IOException, PermissionDeniedException, UnrecoverableKeyException, InvalidKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IllegalStateException, NoSuchProviderException, SignatureException, AuthenticationFailureException, InterruptedException
    {
        BasicConfigurator.configure();

        IndelibleFSFuseMain tester = new IndelibleFSFuseMain();
        String [] myArgs = {"/tmp/src1", "/tmp/mnt1", "-f", "-s", "-d"};
        File srcDir = new File(myArgs[0]);
        if (!srcDir.exists())
            srcDir.mkdir();
        File mntDir = new File(myArgs[1]);
        if (!mntDir.exists())
            mntDir.mkdir();
        @SuppressWarnings("unused")
		ClientFile startFile = SystemInfo.getSystemInfo().getClientFileForFile(srcDir); // Get the volume loop running properly
        tester.start(myArgs);
        tester.setupMount();
        tester.startLoop();
		while (true)
		{
			try
			{
				Thread.sleep(60000);
			}
			catch (InterruptedException e)
			{
				
			}
		}
    }
    IndelibleFSServer fsServer;
    IndelibleFSVolumeIF mountVolume;
    IndelibleDirectoryNodeIF root;
    IndelibleServerConnectionIF connection;
    DataMoverSession moverSession;
    
    public void start(String [] args) throws PermissionDeniedException, IOException, UnrecoverableKeyException, InvalidKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IllegalStateException, NoSuchProviderException, SignatureException, AuthenticationFailureException, InterruptedException
    {
        BasicConfigurator.configure();
        IndelibleServerPreferences.initPreferences(null);
        MonitoredProperties serverProperties = IndelibleServerPreferences.getProperties();
        GeneratorIDFactory genIDFactory = new GeneratorIDFactory();
        GeneratorID testBaseID = genIDFactory.createGeneratorID();
        ObjectIDFactory oidFactory = new ObjectIDFactory(testBaseID);
        File preferencesDir = new File(serverProperties.getProperty(IndelibleServerPreferences.kPreferencesDirPropertyName));
        File securityClientKeystoreFile = new File(preferencesDir, IndelibleFSClient.kIndelibleEntityAuthenticationClientConfigFileName);
        EntityAuthenticationClient.initializeEntityAuthenticationClient(securityClientKeystoreFile, oidFactory, serverProperties);
        IndelibleFSClient.start(null, serverProperties);
        IndelibleFSServer[] servers = new IndelibleFSServer[0];
        
        while(servers.length == 0)
        {
            servers = IndelibleFSClient.listServers();
            if (servers.length == 0)
                Thread.sleep(1000);
        }
        fsServer = servers[0];
        
        String moverPortStr = serverProperties.getProperty(IndelibleServerPreferences.kMoverPortPropertyName);
        int moverPort = Integer.parseInt(moverPortStr);
        
        String localPortDirStr = serverProperties.getProperty(IndelibleServerPreferences.kLocalPortDirectory);
        File localPortDir = new File(localPortDirStr);
        if (localPortDir.exists() && !localPortDir.isDirectory())
        {
        	localPortDir.delete();
        }
        if (!localPortDir.exists())
        {
        	localPortDir.mkdirs();
        }
        File localPortSocketFile = new File(localPortDir, "dataMover");
        // Should have the CAS server and the entity authentication server and client configured by this point
        DataMoverReceiver.init(oidFactory);
        DataMoverSource.init(oidFactory, new InetSocketAddress(moverPort),
        		new AFUNIXSocketAddress(localPortSocketFile, moverPort));   // TODO - move this someplace logical

        connection = fsServer.open();
        connection.startTransaction();
        mountVolume = connection.createVolume(null);
        IndelibleDirectoryNodeIF rootNode = mountVolume.getRoot();
		LinuxFileMetaData mdProperties = new LinuxFileMetaData();
		Date now = new Date();
		mdProperties.setAccessTime(now);
		mdProperties.setChangeTime(now);
		mdProperties.setModifyTime(now);
		mdProperties.setOwner(0);
		mdProperties.setGroup(0);
		mdProperties.setFileType(LinuxFileMetaData.kDirectoryType);
		mdProperties.setAccessMask(0755);
		LinuxFileMetaDataProperties storeProperties = mdProperties.getProperties();
		rootNode.setMetaDataResource(IndelibleFileLike.kClientFileMetaDataPropertyName, storeProperties.getMap());

        connection.commit();
        EntityAuthentication serverID = connection.getServerEntityAuthentication();
        moverSession = DataMoverSource.getDataMoverSource().createDataMoverSession(fsServer.getSecurityServerID());
        moverSession.addAuthorizedClient(serverID);
        
        /*
        IndelibleFSFuseOps ops = new IndelibleFSFuseOps(mountVolume);
        FuseJNAGlue glue = new FuseJNAGlue(ops);
        Pointer user_data = null;
        fl.fuse_main_real(args.length, args, glue, new NativeSize(glue.size()), user_data);
        */
        
		//Loopback loopback = new Loopback(new File("/home"));
        IndelibleFSFUSEVolume indelibleFSPlugin = new IndelibleFSLinuxVolume(connection, mountVolume);
		FUSEInodeAdapter<IndelibleFSInode, IndelibleFSInodeManager, IndelibleFSFileHandle, IndelibleFSDirHandle, IndelibleFSHandleManager> adapter = new FUSEInodeAdapter<IndelibleFSInode, IndelibleFSInodeManager, IndelibleFSFileHandle, IndelibleFSDirHandle, IndelibleFSHandleManager>(indelibleFSPlugin);
		lowLevelFS = adapter;

    }
}
