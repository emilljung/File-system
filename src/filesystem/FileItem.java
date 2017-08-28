package filesystem;

import java.io.*;

/**
 * Used to save and load data to a file.
 * @Version 0.11
 * @author Emil Ljung <emil_ljung1991@hotmail.com>
 */

public class FileItem<T> {
    private File file;
    
    public FileItem() {}
    
    public void saveToFile(T data, String fileName) {
        this.file = new File(fileName);
        
        //Försök att spara arrayen till "file".
        try {
            if(!file.exists()) {
                System.out.printf("\nSkapar ny fil.");
                file.createNewFile();
            }
            
            try(FileOutputStream FOS = new FileOutputStream(file)) {
                ObjectOutputStream OOS = new ObjectOutputStream(FOS);
                
                //Lägg hela arrayen i ObjectOutputStream, som sedan sparar
                //arrayen till "file" tack vare FileOutputStream.
                OOS.writeObject(data);
                
                OOS.close();
            }
        }
        catch(Exception e) {
            //Arrayern kunde, av någon anledning, inte sparas.
            System.out.printf("Det gick inte att spara filen!");
        }
    }
    
    public T loadFromFile(String fileName) {
        T data = null;

        this.file = new File(fileName);
        
        //Försöka att läsa in arrayen från "file".
        try(FileInputStream FIS = new FileInputStream(file)) {
            ObjectInputStream OIS = new ObjectInputStream(FIS);
                
            //Lägg arrayen från filen till byte[][]-arrayen som heter "array".
            data = (T)OIS.readObject();
                
            OIS.close();
        }
        catch(Exception e) {
            if(!file.exists())
                System.out.printf("\nFilen existerar inte!");
            else
                System.out.printf("\nDet gick inte att ladda filen!");
        }
        
        return data;
    }
}