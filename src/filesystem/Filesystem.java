package filesystem;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Initializes the program.
 * @Version 0.11
 * @author Emil Ljung <emil_ljung1991@hotmail.com>
 */

public class Filesystem
{
    //Skal att implementera funktionerna i. 
    //Det är här det är tänkt att du ska skriva det mesta av din kod.
    
    private final BlockDevice m_BlockDevice;
    private String whereAmI;

    public Filesystem(BlockDevice p_BlockDevice)
    {
        this.m_BlockDevice = p_BlockDevice;
        byte[] empty = new byte[512];
        this.m_BlockDevice.createAndSaveData("root", '1', "-1", empty, 0);
        this.whereAmI = "root";
    }

    //bygger upp ett tomt system (”formaterar disken”)
    public String format()
    {
        this.m_BlockDevice.format();
        this.whereAmI = "root";
        return "Diskformat sucessfull";
    }

    //listar innehållet i en katalog
    public String ls(String[] p_asPath)
    {
        //Kolla om filerna existerar, har rätt barn och är av typen '1'.
        //Om så är fallet, skriv ut tillhörande barns namn.
        
        int parent;
        //Kolla om enbart root har skrivits
        if(p_asPath[0].equals("root") && p_asPath.length == 1)
            parent = 0;
        else
            parent = this.m_BlockDevice.checkIfPathExists(p_asPath, 0);
            
        if(parent != -1)
        {
            String[] folderContent = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(parent));
            String referenceList = folderContent[3];
            
            //Kolla om referenslistan är tom, är den det så händer inget
            if(referenceList.length() > 0)
            {
                ArrayList<String> list = new ArrayList<>();

                String nr = "";
                for(int i = 0; i < referenceList.length(); i++)
                {
                    if(referenceList.toCharArray()[i] == '/')
                    {
                        list.add(nr);
                        nr = "";
                    }
                    else
                        nr = nr + referenceList.toCharArray()[i];
                }

                System.out.print("Listing directory ");
                dumpArray(p_asPath);
                for(int i = 0; i < list.size(); i++)
                {
                    String[] content = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(Integer.parseInt(list.get(i))));
                    System.out.print('\n'+content[0]);
                }
            }
            else
                System.out.print("Mappen " + folderContent[0] + " är tom.");
        }
        else
            System.out.print("Pathen finns inte.");
        
        System.out.print("");
        return "";
    }

    //skapar en fil (datainnehållet skrivs in på en extra tom rad)
    public String create(String[] p_asPath, byte[] p_abContents)
    {      
        //Kolla om mapparna existerar, har rätt barn, där alla utom den sista är av typen '1'
        //och om filnamnet som ska sparas redan existerar i mappen där den ska sparas.
        int parent = this.m_BlockDevice.checkIfPathExists(p_asPath, 1);
        if(parent != -1)
        {
            //Hämta förälderns innehåll.
            String[] parentContent = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(parent));
            
            //Kolla om namnet på den nya filens namn redan existerar i mappen där den ska sparas.
            if(!this.m_BlockDevice.checkIfNameExistsInFolder(parentContent, p_asPath[p_asPath.length-1]))
            {
                //Försök att lägga till filen p_asPath[p_asPath.length-1]
                int spot = this.m_BlockDevice.whichBlockToWrite();
                                
                if(this.m_BlockDevice.createAndSaveData(p_asPath[p_asPath.length-1], '0', 
                        Integer.toString(parent), p_abContents, spot) == -2)
                    System.out.print("Namnet är för långt!");
                else
                {                
                    //Lägg till referens i dess förälder.
                    String referenceList = parentContent[3];

                    referenceList = referenceList + Integer.toString(spot)+"/";

                    //Stoppa in de nya värderna i en byte[512] och kasta in i förälderns plats.
                    byte[] newData = new byte[490];

                    for(int i = 0; i < referenceList.length(); i++)
                        if(i >= referenceList.length())
                            newData[i] = (byte)'\0';
                        else
                            newData[i] = (byte)referenceList.toCharArray()[i];

                    this.m_BlockDevice.createAndSaveData(parentContent[0], parentContent[1].toCharArray()[0], parentContent[2], newData, parent);

                    //Kolla så att föräldern faktiskt får en ny referens
                    //String n = new String(this.m_BlockDevice.readBlock(parent));
                    //System.out.println(n);

                    System.out.print("Creating file ");
                    dumpArray(p_asPath);
                }
            }
            else
                System.out.print("Det finns redan en fil/mapp med namnet "+ p_asPath[p_asPath.length-1] +" i mappen "+ parentContent[0]);
            
        }
        else
            System.out.print("Pathen finns inte.");
        
        System.out.print("");
        return "";
    }

    //skriver ut innehållet i en fil på skärmen
    public String cat(String[] p_asPath)
    {
        //Kolla om filen existerar och är av typen '0'.
        //Om så är fallet, skriv ut all data.
   
        int parent = this.m_BlockDevice.checkIfPathExists(p_asPath, 1);
        if(parent != -1)
        {
            String[] folderContent = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(parent));
            
            //Kolla om referenslistan är tom, är den det så händer inget
            if(folderContent[3].length() > 0)
            {
                int[] list = this.m_BlockDevice.splitDataContent(folderContent[3]);
                boolean found = false;
                
                for(int i = 0; i < list.length; i++)
                {
                    //Plocka ut barnets värden
                    String[] content = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(list[i]));
                    
                    //Kolla om barnets namn är p_asPath[p_asPath.length-1] och är av typen '0'
                    if(content[0].equals(p_asPath[p_asPath.length-1]) && content[1].equals("0"))
                        found = true;
                    
                    if(found == true)
                    {
                        //Skriv ut filens text
                        System.out.print("Dumping contents of file ");
                        dumpArray(p_asPath);
                        System.out.print("\n"+content[3]);
                        break;
                    }
                }
                
                if(found == false)
                    System.out.print("Filen hittades ej.");
            }
            else
                System.out.print("Mappen " + folderContent[0] + " är tom.");
        }
        else
            System.out.print("Pathen finns inte.");
        
        System.out.print("");
        return "";
    }

    //sparar systemet på en fil
    public String save(String p_sPath)
    {
        //DET RÄCKER ATT MAN ENBART SKRIVER FILNAMN!!
        System.out.print("Saving blockdevice to file " + p_sPath);
        this.m_BlockDevice.saveSystem(p_sPath);
        return "";
    }

    //återställer systemet från en fil
    public String read(String p_sPath)
    {
        //DET RÄCKER ATT MAN ENBART SKRIVER FILNAMN!!
        System.out.print("Reading file " + p_sPath + " to blockdevice");
        this.m_BlockDevice.loadSystem(p_sPath);
        return "";
    }

    //tar bort en fil
    public String rm(String[] p_asPath)
    { 
        int parent = this.m_BlockDevice.checkIfPathExists(p_asPath, 1);
        if(parent != -1)
        {
            String[] folderContent = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(parent));
            
            //Kolla om referenslistan är tom, är den det så händer inget
            if(folderContent[3].length() > 0)
            {
                int[] list = this.m_BlockDevice.splitDataContent(folderContent[3]);
                boolean found = false;
                
                for(int i = 0; i < list.length; i++)
                {
                    //Plocka ut barnets värden
                    String[] content = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(list[i]));
                    
                    //Kolla om barnets namn är p_asPath[p_asPath.length-1] och är av typen '0'
                    if(content[0].equals(p_asPath[p_asPath.length-1]) && content[1].equals("0"))
                        found = true;
                    
                    if(found == true)
                    {
                        //Ta bort fil och ta bort referens i föräldern
                        System.out.print("Removing file ");
                        dumpArray(p_asPath);
                        
                        byte[] empty = new byte[512];
                        for(int j = 0; j < 512; j++)
                            empty[j] = (byte)'\0';
                        
                        //Ger den borttagna platsen en byte-array som är tom
                        this.m_BlockDevice.writeBlock(list[i], empty);
                        
                        //Skapa ny refereslista till föräldern
                        String newList = "";
                        for(int j = 0; j < list.length; j++)
                            if(list[i] != list[j])
                                newList = newList + Integer.toString(list[j])+"/";

                        byte[] newData = new byte[490];

                        //Stoppa in den nya referenslistan i byte[]
                        for(int j = 0; j < newList.length(); j++)
                            if(j >= newList.length())
                                newData[j] = (byte)'\0';
                            else
                                newData[j] = (byte)newList.toCharArray()[j];
                        
                        //Ändra föräldermappen
                        this.m_BlockDevice.createAndSaveData(folderContent[0], folderContent[1].toCharArray()[0], folderContent[2], newData, parent);
                        
                        break;
                    }
                }
                
                if(found == false)
                    System.out.print("Filen hittades ej.");
            }
            else
                System.out.print("Mappen " + folderContent[0] + " är tom.");
        }
        else
            System.out.print("Pathen finns inte.");
        
        System.out.print("");
        return "";
    }

    //kopierar en fil
    public String copy(String[] p_asSource, String[] p_asDestination)
    {
        //Kolla om filen och destinationen existerar.
        //Om de gör det så skapa en kopia av originalet fast med destinationens förälderID.
        int parent = this.m_BlockDevice.checkIfPathExists(p_asSource, 1);
        
        if(parent != -1)
        {
            String[] folderContent = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(parent));
            
            //Kolla om referenslistan är tom, är den det så händer inget
            if(folderContent[3].length() > 0)
            {
                int[] list = this.m_BlockDevice.splitDataContent(folderContent[3]);
                boolean found = false;
                
                for(int i = 0; i < list.length; i++)
                {
                    //Plocka ut barnets värden
                    String[] content = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(list[i]));
                    
                    //Kolla om barnets namn är p_asSource[p_asSource.length-1] och är av typen '0'
                    if(content[0].equals(p_asSource[p_asSource.length-1]) && content[1].equals("0"))
                        found = true;
                    
                    if(found == true)
                    {
                        //p_asSource existerar. Kolla om p_asDestination gör det också.
                        int nextParent;
                        if(p_asDestination[0].equals("root") && p_asDestination.length == 1)
                            nextParent = 0;
                        else
                            nextParent = this.m_BlockDevice.checkIfPathExists(p_asDestination, 0);
                        
                        if(nextParent != -1)
                        {
                            //Den nya (eller?) förälderns värden
                            String[] nextParentContent = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(nextParent));
                            
                            if(!this.m_BlockDevice.checkIfNameExistsInFolder(nextParentContent, content[0]))
                            {
                                byte[] data = new byte[490];

                                for(int j = 0; j < content[3].length(); j++)
                                    data[j] = (byte)content[3].toCharArray()[j];

                                int id = this.m_BlockDevice.whichBlockToWrite();

                                //Eftersom att originalet har godkänts så behöver jag inte kontrollera namnet.
                                this.m_BlockDevice.createAndSaveData(content[0], content[1].toCharArray()[0], 
                                                    Integer.toString(nextParent), data, id);

                                //content har nu värden från kopians förälder
                                String referenceList = nextParentContent[3];

                                referenceList = referenceList + Integer.toString(id)+ "/";

                                //Stoppa in de nya värderna i en byte[512] och kasta in i förälderns plats.
                                byte[] newData = new byte[490];

                                for(int j = 0; j < referenceList.length(); j++)
                                    if(j >= referenceList.length())
                                        newData[j] = (byte)'\0';
                                    else
                                        newData[j] = (byte)referenceList.toCharArray()[j];

                                //Ändra referenslistan i kopians förälder
                                this.m_BlockDevice.createAndSaveData(nextParentContent[0], nextParentContent[1].toCharArray()[0], nextParentContent[2], newData, nextParent);

                                System.out.print("Copying file from ");
                                dumpArray(p_asSource);
                                System.out.print(" to ");
                                dumpArray(p_asDestination);
                                break;
                            }
                            else
                                System.out.print("Det finns redan en fil/mapp med namnet "+ content[0] +" i mappen "+ nextParentContent[0]);

                        }
                        else
                        {
                            System.out.println("Den andra pathen finns ej.");
                            break;
                        }
                    }
                }
                
                if(found == false)
                    System.out.print("Filen hittades ej.");
            }
            else
                System.out.print("Mappen " + folderContent[0] + " är tom.");
        }
        else
            System.out.print("Pathen finns inte.");
        
        System.out.print("");
        return "";
    }

    //lägger till innehåll i slutet på filen
    public String append(String[] p_asSource)
    {
        //Skriv en path till en fil (inte mapp). 
        int parent = this.m_BlockDevice.checkIfPathExists(p_asSource, 1);
        if(parent != -1)
        {
            String[] folderContent = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(parent));
            
            //Kolla om referenslistan är tom, är den det så händer inget
            if(folderContent[3].length() > 0)
            {
                int[] list = this.m_BlockDevice.splitDataContent(folderContent[3]);
                boolean found = false;
                
                for(int i = 0; i < list.length; i++)
                {
                    //Plocka ut barnets värden
                    String[] content = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(list[i]));
                    
                    //Kolla om barnets namn är p_asSource[p_asSource.length-1] och är av typen '0'
                    if(content[0].equals(p_asSource[p_asSource.length-1]) && content[1].equals("0"))
                        found = true;
                    
                    if(found == true)
                    {
                        //Skriv in text
                        Scanner sc = new Scanner(System.in);
                        String text = sc.next(), newText = "";
                        
                        //Dessa 2 loopas görs för att '\n' inte ska komma med i resultatet
                        for(int j = 0; j < content[3].length(); j++)
                            if(content[3].toCharArray()[j] != '\n')
                                newText = newText+content[3].toCharArray()[j];
                        
                        for(int j = 0; j < text.length(); j++)
                            if(text.toCharArray()[j] != '\n')
                                newText = newText+text.toCharArray()[j];
                        
                        byte[] data = new byte[490];
                        
                        for(int j = 0; j < newText.length(); j++)
                            data[j] = (byte)newText.toCharArray()[j];
                                                
                        this.m_BlockDevice.createAndSaveData(content[0], content[1].toCharArray()[0], 
                                Integer.toString(parent), data, list[i]);
                        
                        System.out.print("Appending file ");
                        dumpArray(p_asSource);
                        break;
                    }
                }
                
                if(found == false)
                    System.out.print("Filen hittades ej.");
            }
            else
                System.out.print("Mappen " + folderContent[0] + " är tom.");
        }
        else
            System.out.print("Pathen finns inte.");

        System.out.print("");
        return "";
    }

    //ändrar namn på filen
    public String rename(String[] p_asSource, String[] p_asDestination)
    {
        //RÄCKER ATT MAN ENBART SKRIVER "NyttNamn" I p_asDestination
        
        //Kolla om filen och om det nya namnet är ledigt.
        //Om filen finns och om namnet är ledigt så få tag
        //på vilket ID filen har, ändra namnet på filen.
        
        int parent = this.m_BlockDevice.checkIfPathExists(p_asSource, 1);
        if(parent != -1)
        {
            String[] folderContent = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(parent));
            
            //Kolla om referenslistan är tom, är den det så händer inget
            if(folderContent[3].length() > 0)
            {
                int[] list = this.m_BlockDevice.splitDataContent(folderContent[3]);
                boolean found = false;
                
                for(int i = 0; i < list.length; i++)
                {
                    //Plocka ut barnets värden
                    String[] content = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(list[i]));
                    String n = new String(this.m_BlockDevice.readBlock(list[i]));
                    
                    //Kolla om barnets namn är p_asSource[p_asSource.length-1] och är av typen '0'
                    if(content[0].equals(p_asSource[p_asSource.length-1]) && content[1].equals("0"))
                        found = true;
                    
                    if(found == true)
                    {
                        //Byt namn på fil
                        
                        byte[] data = new byte[490];
                                                
                        for(int j = 0; j < content[3].length(); j++)
                            data[j] = (byte)content[3].toCharArray()[j];
                        
                        this.m_BlockDevice.createAndSaveData(p_asDestination[0], content[1].toCharArray()[0], Integer.toString(parent), data, list[i]);
                        
                        System.out.print("Renaming file ");
                        dumpArray(p_asSource);
                        System.out.print(" to ");
                        dumpArray(p_asDestination);
                        break;
                    }
                }
                
                if(found == false)
                    System.out.print("Filen hittades ej.");
            }
            else
                System.out.print("Mappen " + folderContent[0] + " är tom.");
        }
        else
            System.out.print("Pathen finns inte.");
        
        System.out.print("");
        return "";
    }

    //skapar en ny tom katalog
    public String mkdir(String[] p_asPath)
    {
        //Kolla om mapparna existerar, har rätt barn, där alla är av typen '1'
        //Om så är fallet, försök att lägga till mappen p_asPath[p_asPath.length-1]
        
        int parent = this.m_BlockDevice.checkIfPathExists(p_asPath, 1);
        if(parent != -1)
        {
            //Hämta förälderns innehåll.
            String[] parentContent = this.m_BlockDevice.getContent(this.m_BlockDevice.readBlock(parent));
            
            //Kolla om namnet på den nya mappens namn redan existerar i mappen där den ska sparas
            if(!this.m_BlockDevice.checkIfNameExistsInFolder(parentContent, p_asPath[p_asPath.length-1]))
            {
                //Försök att lägga till filen p_asPath[p_asPath.length-1]
                int spot = this.m_BlockDevice.whichBlockToWrite();

                byte[] emptyData = new byte[490];
                for(int i = 0; i < 490; i++)
                    emptyData[i] = (byte)'\0';

                if(this.m_BlockDevice.createAndSaveData(p_asPath[p_asPath.length-1], '1', 
                        Integer.toString(parent), emptyData, spot) == -2)
                    System.out.print("Namnet är för långt!");
                else
                {
                    String referenceList = parentContent[3];

                    referenceList = referenceList + Integer.toString(spot)+ "/";

                    //Stoppa in de nya värderna i en byte[512] och kasta in i förälderns plats.
                    //(String name, char type, String parent, byte[] data, int spot)
                    byte[] newData = new byte[490];

                    for(int i = 0; i < referenceList.length(); i++)
                        if(i >= referenceList.length())
                            newData[i] = (byte)'\0';
                        else
                            newData[i] = (byte)referenceList.toCharArray()[i];

                    this.m_BlockDevice.createAndSaveData(parentContent[0], parentContent[1].toCharArray()[0], parentContent[2], newData, parent);

                    System.out.print("Creating directory ");
                    dumpArray(p_asPath);
                }
            }
            else
                System.out.print("Det finns redan en fil/mapp med namnet "+ p_asPath[p_asPath.length-1] +" i mappen "+ parentContent[0]);
        }
        else
            System.out.print("Pathen finns inte.");
        
        System.out.print("");
        return "";
    }

    //ändrar aktuell katalog
    public String cd(String[] p_asPath)
    {
        //Kolla om mappen man försöker gå existerar. Gör den inte det så ändras inte whereAmI,
        //annars så sätts whereAmI till en sträng av p_asPath.
        
        //Kolla om man ska gå till en ovanstående mapp mha relativ sökväg eller ej
        if(p_asPath.length == 1)
        {
            ArrayList<String> list = new ArrayList<>();
            String folderName = "";
            boolean pathFound = false;
            
            for(int i = 0; i < this.whereAmI.length(); i++)
            {
                //Plocka ut alla ovanstående mappar som finns i whereAmI (pwd).
                //Den sista mappen, dvs den man är i, kommer inte med!
                if(this.whereAmI.toCharArray()[i] == '/')
                {
                    list.add(folderName);
                    
                    //Kolla om mappen man vill till har hittats.
                    if(p_asPath[0].equals(folderName))
                    {
                        //De värden som finns i list blir den nya pathen.
                        pathFound = true;
                        this.whereAmI = "root";
                        for(int j = 1; j < list.size(); j++)
                            this.whereAmI = this.whereAmI + "/" + list.get(j);
                        
                        //Detta innebär att är man i mappen root/mapp1/root/mapp2 och skriver "cd root"
                        //så kommer man att hamna i den root som är längst uppe i den pathen.
                        
                        System.out.print("Changing directory to ");
                        dumpArray(list.toArray(new String[list.size()-1]));
                        
                        break;
                    }
                    else
                        folderName = "";
                }
                else
                    folderName = folderName + this.whereAmI.toCharArray()[i];
            }
            
            if(pathFound == false)
                System.out.println("Det finns ingen ovanstående mapp som heter " + p_asPath[0]);
        }
        else
        {
            //Tror av någon anledning att den sista mappen som man ska gå till som är tom
            //gör att pathen inte existerar.
            int parent = this.m_BlockDevice.checkIfPathExists(p_asPath, 0);
            
            if(parent != -1)
            {
                System.out.print("Changing directory to ");
                dumpArray(p_asPath);
                this.whereAmI = "root";
                
                //Svaret blir i formen "root/ko/nisse/häst/.."
                for(int i = 1; i < p_asPath.length; i++)
                    this.whereAmI = this.whereAmI + "/" + p_asPath[i];
            }
            else
            {
                System.out.print("Pathen finns inte.");
            }
        }
        
        System.out.print("");
        return ""; 
    }

    //skriver namn på aktuell katalog på skärmen
    public String pwd()
    {
        return this.whereAmI;
    }

    private void dumpArray(String[] p_asArray)
    {
        for(int nIndex = 0; nIndex < p_asArray.length; nIndex++)
        {
            System.out.print(p_asArray[nIndex] + "=>");
        }
    }
}