package filesystem;

import java.io.Serializable;

/**
 * The function declarations which are 
 * defined in MemoryBlockDevice.java.
 * @Version 1.0
 * @author Emil Ljung <emil_ljung1991@hotmail.com>
 */

public abstract class BlockDevice implements Serializable {
    public BlockDevice() {}

    public abstract int writeBlock(int p_nBlockNr, byte[] p_abContents);
    public abstract byte[] readBlock(int p_nBlockNr);
    public abstract int whichBlockToWrite();
    public abstract int createAndSaveData(String name, char type, String parent, byte[] data, int spot);
    public abstract void format();
    public abstract void saveSystem(String fileName);
    public abstract void loadSystem(String fileName);
    
    public abstract int checkIfPathExists(String[] path, int value);
    public abstract String[] getContent(byte[] data);
    public abstract int[] splitDataContent(String data);
    public abstract boolean checkIfNameExistsInFolder(String[] parentContent, String name);

}