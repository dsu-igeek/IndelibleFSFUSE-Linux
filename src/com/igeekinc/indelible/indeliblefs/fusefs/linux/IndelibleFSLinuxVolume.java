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

import com.igeekinc.indelible.indeliblefs.IndelibleFSVolumeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleFileNodeIF;
import com.igeekinc.indelible.indeliblefs.IndelibleServerConnectionIF;
import com.igeekinc.indelible.indeliblefs.exceptions.PermissionDeniedException;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSFUSEVolume;
import com.igeekinc.indelible.indeliblefs.fusefs.IndelibleFSInode;
import com.igeekinc.luwak.FUSEAttr;
import com.igeekinc.luwak.inode.exceptions.InodeException;
import com.igeekinc.luwak.inode.exceptions.InodeIOException;
import com.igeekinc.luwak.inode.exceptions.PermissionException;

public class IndelibleFSLinuxVolume extends IndelibleFSFUSEVolume 
{

	public IndelibleFSLinuxVolume(IndelibleServerConnectionIF connection, IndelibleFSVolumeIF indelibleFSVolume) 
	{
		super(connection, indelibleFSVolume);
	}

	@Override
	public IndelibleFSInode createInode(IndelibleFileNodeIF lookupFile, int inodeNum, int generation) throws InodeException 
	{
		IndelibleFSLinuxInode returnInode;
		FUSEAttr attr;
		try {
			attr = IndelibleFSLinuxInode.attrForFileStatic(lookupFile, inodeNum);
		} catch (RemoteException e) {
			throw new InodeIOException();
		} catch (PermissionDeniedException e) {
			throw new PermissionException();
		}  catch (IOException e) {
			throw new InodeIOException();
		}
		try {
			returnInode = new IndelibleFSLinuxInode(this, lookupFile, inodeNum, generation, attr);
		} catch (IOException e) {
			throw new InodeIOException();
		} catch (PermissionDeniedException e) {
			throw new PermissionException();
		}
		getInodeManager().addInode(returnInode);
		return returnInode;
	}
}
