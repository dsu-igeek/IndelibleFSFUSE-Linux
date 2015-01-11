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

import org.apache.log4j.Logger;

import com.igeekinc.indelible.indeliblefs.IndelibleDirectoryNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSFUSEAttr;
import com.igeekinc.luwak.inode.exceptions.InodeIOException;
import com.igeekinc.util.ClientFileMetaDataProperties;
import com.igeekinc.util.linux.LinuxFileMetaData;
import com.igeekinc.util.linux.LinuxOSConstants;
import com.igeekinc.util.logging.ErrorLogMessage;
import com.igeekinc.util.unix.UnixDate;

public class IndelibleFSFUSELinuxAttr extends IndelibleFSFUSEAttr
{

	
	public IndelibleFSFUSELinuxAttr(IndelibleFileNodeIF node, ClientFileMetaDataProperties mdProperties) throws InodeIOException
	{
		try
		{
			LinuxFileMetaData md = (LinuxFileMetaData) mdProperties.getMetaData();
			setATime(new UnixDate(md.getAccessTime()));
			setCTime(new UnixDate(md.getChangeTime()));
			setMTime(new UnixDate(md.getModifyTime()));
			
			

			int accessMask = md.getAccessMask();
			
			// We only have directories and regular files in Indelible FS at the moment
			if (md.getFileType() == LinuxFileMetaData.kDirectoryType)
			{
				accessMask |= LinuxOSConstants.S_IFDIR;
				setSize((((IndelibleDirectoryNodeIF)node).getNumChildren() + 2) * 512);
				setNLink(((IndelibleDirectoryNodeIF)node).getNumChildren() + 2);
			}
			else
			{
				accessMask |= LinuxOSConstants.S_IFREG;
				setSize(node.totalLength());
				setNLink(/*node.getReferenceCount()*/1);
			}
			setMode(accessMask);
			setBlockSize(4096);
			long blocks = (getSize() % 512 != 0) ? getSize() / 512 + 1: getSize()/512;
			setBlocks(blocks);
			
		} catch (PermissionDeniedException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InodeIOException();
		} catch (IOException e)
		{
			Logger.getLogger(getClass()).error(new ErrorLogMessage("Caught exception"), e);
			throw new InodeIOException();
		}
		
	}
}
