package filesystem;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * The functions which are used with the shell commands.
 * @Version 0.11
 * @author Emil Ljung <emil_ljung1991@hotmail.com>
 */

public class MemoryBlockDevice extends BlockDevice
{
    //Definierar minnesblock om 512 byte och operationer för att läsa
    //och skriva data till och från dem

    byte[][] m_abContents = new byte[250][512];         //Här sparas all data. Tom plats = 0
    
    FileItem<byte[][]> byteFile = new FileItem<>();     //Är till för att spara/ladda all data
    
    @Override
    public int writeBlock(int p_nBlockNr, byte[] p_abContents)
    {
        if(p_nBlockNr > 249 || p_nBlockNr < 0)
            return -1; // Block out-of-range
        
        System.arraycopy(p_abContents, 0, m_abContents[p_nBlockNr], 0, 512);
        
        return 1;
    }

    @Override
    public byte[] readBlock(int p_nBlockNr)
    {
        if(p_nBlockNr > 249 || p_nBlockNr < 0)
            return new byte[0]; // Block out-of-range

        byte[] abBlock = new byte[512];
       
        //for(int nIndex = 0; nIndex < 512; nIndex++){abBlock[nIndex] = m_abContents[p_nBlockNr][nIndex];}
        System.arraycopy(m_abContents[p_nBlockNr], 0, abBlock, 0, 512);

        return abBlock; 
    }
    
    @Override
    public int whichBlockToWrite()
    {
        int ans = -1;
        
        for(int i = 0; i < 250; i++)
            if(this.m_abContents[i][0] == 0)
            {
                ans = i;
                break;
            }
        
        return ans;
    }
    
    @Override
    public int createAndSaveData(String name, char type, String parent, byte[] data, int spot)
    {
        byte[] result = new byte[512];
        
        if(name.length() > 15) //data.length != 512 ||  Block size out-of-range
            return -2; //namn är för långt
        
        //Stoppa in name och fyll ut tom plats med '\0'
        for(int i = 0; i < 15; i++)
        {
            if(i >= name.length())
                result[i] = (byte)'\0';
            else
                result[i] = (byte)name.toCharArray()[i];
        }
        
        //Vilken typ av fil plus tom plats
        result[16] = (byte)type;    //'0'=fil, '1'=mapp 
        result[17] = (byte)'\0';
        
        //Stoppa in referens till föräldern och fyll ut med '\0' (detta krånglade lite)
        char[] q = new char[4];
        for(int i = 0; i < 4; i++)
            if(i >= parent.length())
            {
                q[i] = '\0';
                result[i+18] = (byte)q[i];
            }
            else
            {
                q[i] = parent.toCharArray()[i];
                result[i+18] = (byte)q[i];
            }
        
        //Stoppa in datan.
        //För fil är det text som man skriver in och för mapp så är det en lista på dess barn.
        for(int i = 22; i < 512; i++)
            result[i] = data[i-22];
        
        //Kolla så att datan faktiskt är korrekt
        //String n = new String(result);
        //System.out.println(n + '\n' + this.whichBlockToWrite());
        
        this.writeBlock(spot, result);
        
        return 1;
    }
        
    @Override
    public void format()
    {
        this.m_abContents = null;
        this.m_abContents = new byte[250][512];
        byte[] empty = new byte[512];
        this.createAndSaveData("root", '1', "-1", empty, 0);
    }
    
    @Override
    public void saveSystem(String fileName)
    {
        this.byteFile.saveToFile(this.m_abContents, fileName);
    }
    
    @Override
    public void loadSystem(String fileName)
    {
        this.m_abContents = this.byteFile.loadFromFile(fileName);
    }
    
    @Override
    public int checkIfPathExists(String[] path, int value)
    {
        int parent = -1; //Returneras -1 så existerar inte pathen.
        
        byte[] data = new byte[512];
        System.arraycopy(this.m_abContents[0], 0, data, 0, 512);
                
        //[0]=namn, [1]=typ, [2]=förälder, [3] data för m_abContents[0], dvs root
        String[] content = this.getContent(data);
        
        //Kolla om...
        //1. path[0] är i m_abContents[0][...], root ska ALLTID vara där.
        //2. var någonstans pathen slutar. Skrivs "root/fil1" så är det bara root som ska kontrolleras.
        if(path[0].equals(content[0]) && path.length > 1)
        {
            parent = 0; //referens till "root"
            
            //value = 1 då något skapas (den sista behöver man inte kontrollera)
            //value = 0 då man ska kontrollera hela pathen.
            for(int i = 1; i < path.length-value; i++)
            {
                int oldParent = parent;
                String[] parentContent = content;
                int[] refList = this.splitDataContent(parentContent[3]);
                
                //Loopa igenom förälderns referenslista och leta upp namnet i path[i]
                for(int j = 0; j < refList.length; j++)
                {
                    //Plocka ut barnets innehåll
                    content = this.getContent(this.readBlock(refList[j]));
                    //System.out.println(refList[j]);
                    //System.out.println(content[0] + " jämförs med " + path[i] + " och är av typen " +content[1]);
                    
                    if(content[0].equals(path[i]) && content[1].equals("1"))
                    {
                        //System.out.println("Föräldern till "+ content[0] + " ändras till " +refList[j]);
                        parent = refList[j];
                        break;
                    }
                }
                
                if(oldParent == parent)
                {
                    parent = -1;
                    break;
                }
            }
        }        
        
        return parent;
    }
    
    @Override
    public String[] getContent(byte[] data)
    {
        String[] content = new String[4];
        String dataContent = new String(data), info = "";
                        
        //Plocka ut filens/mappens namn
        int count = 0;
        while(dataContent.toCharArray()[count] != '\0')
        {
            info = info + dataContent.toCharArray()[count];
            count++;
        }
        content[0] = info;
        info = "";
        
        //Plocka ut typen
        info = info + dataContent.toCharArray()[16];
        content[1] = info;
        info = "";
        
        //Plocka ut referens till förälder
        count = 18;
        while(dataContent.toCharArray()[count] != '\0')
        {
            info = info + dataContent.toCharArray()[count];
            count++;
        }
        content[2] = info;
        info = "";
        
        //Plocka ut all data
        count = 22;
        while(dataContent.toCharArray()[count] != '\0')
        {
            info = info + dataContent.toCharArray()[count];
            count++;
        }
        content[3] = info;
        
        return content;
    }
    
    @Override
    public int[] splitDataContent(String data)
    {        
        //Datan i mapparna ska i Strängform se ut som "0/32/213/.."
        //Siffrorna står för vilka IDn, alt. x i byte[x][], som hör till den mappen.
        
        String name = "";
        ArrayList<Integer> list = new ArrayList<>();
        
        //Plocka ut alla IDn som tillhör mappen.
        for(int i = 0; i < data.length(); i++)
        {            
            if(data.toCharArray()[i] == '/')
            {
                list.add(Integer.parseInt(name));
                name = "";
            }
            else
                name = name + data.toCharArray()[i];
        }
        
        //Integer -> int
        int[] ret = new int[list.size()];
        Iterator<Integer> iterator = list.iterator();
        for(int i = 0; i < ret.length; i++)
            ret[i] = iterator.next();
        
        //Returnera alla namn i int[]
        return ret;
    }
    
    @Override
    public boolean checkIfNameExistsInFolder(String[] parentContent, String name)
    {
        boolean exists = false;
        
        //Kolla om det finns någon refens.
        if(parentContent[3].length() > 0)
        {
            //Plocka ut alla IDn till filerna och mapparna i föräldern
            int[] data = this.splitDataContent(parentContent[3]);
            
            //Kolla om föräldermappen redan har en fil/mapp med namnet name
            for(int i = 0; i < data.length; i++)
            {
                //System.out.println(this.getContent(this.readBlock(i))[0] + " och " + name);
                if(this.getContent(this.readBlock(data[i]))[0].equals(name))
                {
                    exists = true;
                    break;
                }
            }
        }
        
        return exists;
    }
}