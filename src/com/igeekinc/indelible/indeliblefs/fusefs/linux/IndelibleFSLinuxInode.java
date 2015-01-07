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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import com.igeekinc.indelible.indeliblefs.IndelibleFileLike;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSFUSEVolume;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSInode;
import com.igeekinc.luwak.FUSEAttr;
import com.igeekinc.luwak.inode.FUSEReqInfo;
import com.igeekinc.luwak.inode.SetAttrs;
import com.igeekinc.luwak.inode.exceptions.InodeException;
import com.igeekinc.luwak.inode.exceptions.InodeIOException;
import com.igeekinc.util.ClientFileMetaData;
import com.igeekinc.util.ClientFileMetaDataProperties;
import com.igeekinc.util.linux.LinuxFileMetaData;
import com.igeekinc.util.linux.LinuxOSConstants;
import com.igeekinc.util.unix.UnixDate;

public class IndelibleFSLinuxInode extends IndelibleFSInode 
{

	public IndelibleFSLinuxInode(IndelibleFSFUSEVolume indelibleFSVolume,
			IndelibleFileNodeIF node, long inodeNum, long generation, FUSEAttr attr)
			throws InodeIOException, RemoteException, PermissionDeniedException, IOException {
		super(indelibleFSVolume, node, inodeNum, generation, attr);
	}

	public ClientFileMetaData createRegularFileMD(FUSEReqInfo reqInfo, int mode)
	{
		LinuxFileMetaData md = new LinuxFileMetaData();
		
		Date now = new Date();
		md.setAccessTime(now);
		md.setChangeTime(now);
		md.setModifyTime(now);
		md.setOwner(reqInfo.getUID());
		md.setGroup(reqInfo.getGID());
		md.setAccessMask(mode);
		md.setFileType(LinuxFileMetaData.kRegularFileType);
		return md;
	}

	@Override
	public ClientFileMetaData createDirectoryMD(FUSEReqInfo reqInfo, int mode) 
	{
		LinuxFileMetaData md = new LinuxFileMetaData();
		
		Date now = new Date();
		md.setAccessTime(now);
		md.setChangeTime(now);
		md.setModifyTime(now);
		md.setOwner(reqInfo.getUID());
		md.setGroup(reqInfo.getGID());
		md.setAccessMask(mode);
		md.setFileType(LinuxFileMetaData.kDirectoryType);
		return md;
	}

	@Override
	public boolean openForWrite(int flags) 
	{
		return (((flags & LinuxOSConstants.O_ACCMODE) & (LinuxOSConstants.O_WRONLY | LinuxOSConstants.O_RDWR)) != 0);
	}

	@Override
	public boolean openAndTruncate(int flags) {
		// TODO Auto-generated method stub
		return false;
	}

	public FUSEAttr attrForFile(IndelibleFileNodeIF attrFile, long inodeNum) throws InodeException, RemoteException, PermissionDeniedException, IOException
	{
		return attrForFileStatic(attrFile, inodeNum);
	}

	public static FUSEAttr attrForFileStatic(IndelibleFileNodeIF attrFile,
			long inodeNum) throws RemoteException, PermissionDeniedException,
			IOException {
		FUSEAttr returnAttr = new FUSEAttr();
		returnAttr.setInode(inodeNum);
		
		Map<String, Object> metaDataResource = attrFile.getMetaDataResource(IndelibleFileLike.kClientFileMetaDataPropertyName);
		if (metaDataResource != null)
		{
			ClientFileMetaDataProperties properties = ClientFileMetaDataProperties.getPropertiesForMap(metaDataResource);
			ClientFileMetaData md = properties.getMetaData();
			if (md instanceof LinuxFileMetaData)
			{
				LinuxFileMetaData linuxMD = (LinuxFileMetaData)md;
				returnAttr.setMode(linuxMD.getAccessMask());
				returnAttr.setNLink(1);	// TODO - figure out how links work in IndelibleFS :-)
				returnAttr.setUID(linuxMD.getOwnerID());
				returnAttr.setGID(linuxMD.getGroupID());
				returnAttr.setRDev(0);
				returnAttr.setSize(attrFile.totalLength());

				returnAttr.setATime(new UnixDate(linuxMD.getAccessTime()));
				returnAttr.setCTime(new UnixDate(linuxMD.getChangeTime()));
				returnAttr.setMTime(new UnixDate(linuxMD.getModifyTime()));
			}
		}
		else
		{
			// No stored attributes so just make something up
			if (attrFile.isDirectory())
				returnAttr.setMode(0040755);
			else
				returnAttr.setMode(0100755);
			returnAttr.setNLink(1);
			returnAttr.setUID(0);
			returnAttr.setGID(0);
			returnAttr.setRDev(0);
			returnAttr.setSize(attrFile.totalLength());
			returnAttr.setATime(new UnixDate(new Date()));
			returnAttr.setCTime(new UnixDate(new Date()));
			returnAttr.setMTime(new UnixDate(new Date()));
		}
		returnAttr.setBlockSize(512);
		returnAttr.setBlocks(returnAttr.getSize()/512 + (returnAttr.getSize() % 512 == 0?0:1));
		return returnAttr;
	}

	@Override
	public FUSEAttr setAttr(FUSEReqInfo reqInfo, SetAttrs newAttr)
			throws InodeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] listXAttr(FUSEReqInfo reqInfo) throws InodeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getXAttr(FUSEReqInfo reqInfo, String xattrName, int offset,
			byte[] buffer) throws InodeException {
		// TODO Auto-generated method stub
		return 0;
	}
}
