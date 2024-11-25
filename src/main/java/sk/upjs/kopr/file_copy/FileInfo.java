package sk.upjs.kopr.file_copy;

import java.io.Serializable;

public class FileInfo implements Serializable{
	private static final long serialVersionUID = -1361912600329298754L;
	public final String fileName;
	public final long size;
	public FileInfo(String fileName, long size) {
		this.fileName = fileName;
		this.size = size;
	}
	public String getFileName() {
		return fileName;
	}

	public long getFileSize() {
		return size;
	}
}
