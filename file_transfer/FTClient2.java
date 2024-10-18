import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
class FileUploadEvent
{
private long numberOfBytesUploaded;
private String uploaderId;
private File file;
FileUploadEvent()
{
this.file=null;
this.uploaderId=null;
this.numberOfBytesUploaded=0;
}
public void setFile(File file)
{
this.file=file;
}
public File getFile()
{
return this.file;
}
public void setUploaderId(String uploaderId)
{
this.uploaderId=uploaderId;
}
public String getUploaderId()
{
return this.uploaderId;
}
public void setNumberOfBytesUploaded(long numberOfBytesUploaded)
{
this.numberOfBytesUploaded=numberOfBytesUploaded;
}
public long getNumberOfBytesUploaded()
{
return this.numberOfBytesUploaded;
}
}

interface UploadFileListener
{
public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent);
}

class FTModel extends AbstractTableModel
{
private ArrayList<File> arrayList;
FTModel()
{
arrayList=new ArrayList<File>();
}

public int getRowCount()
{
return this.arrayList.size();
}

public int getColumnCount()
{
return 2;
}

public String getColumnName(int c)
{
if(c==0)return "S.No";
return "File";
}

public Class getColumnClass(int c)
{
if(c==0)return Integer.class;
return String.class;
}

public Object getValueAt(int r,int c)
{
if(c==0)return r+1;
return this.arrayList.get(r).getAbsolutePath();
}

public boolean isCellEditable()
{
return false;
}

public void add(File file)
{
this.arrayList.add(file);
fireTableDataChanged();
}

public ArrayList<File> getFiles()
{
return arrayList;
}
}

class FTClientFrame extends JFrame
{
private String host;
private int portNumber;
private Container container;
private FileSelectionPanel fileSelectionPanel;
private FileUploadViewPanel fileUploadedViewPanel;

FTClientFrame(String host,int portNumber)
{
this.host=host;
this.portNumber=portNumber;
fileSelectionPanel=new FileSelectionPanel();
fileUploadedViewPanel=new FileUploadViewPanel();
container=getContentPane();
container.setLayout(new GridLayout(1,2));
container.add(fileSelectionPanel);
container.add(fileUploadedViewPanel);
Dimension d=Toolkit.getDefaultToolkit().getScreenSize();
int y=600;
int x=1200;
setLocation((d.width-x)/2,(d.height-y)/2);
setSize(x,y);
setVisible(true);
setDefaultCloseOperation(EXIT_ON_CLOSE);
}

public static void main(String[]gg)
{
FTClientFrame fcf=new FTClientFrame("localhost",5500);
}

//innerClasses
class FileSelectionPanel extends JPanel implements ActionListener
{
private FTModel model;
private JTable table;
private JScrollPane jsp;
private JButton button;
private JLabel titleLabel;

FileSelectionPanel()
{
titleLabel=new JLabel("Selected Files");
Font labelFont=new Font("Verdana",Font.BOLD,20);
titleLabel.setFont(labelFont);
button=new JButton("Add File");
model=new FTModel();
table=new JTable(model);
table.getColumnModel().getColumn(1).setPreferredWidth(350);
Font headerFont=new Font("Verdana",Font.BOLD,18);
Font tableFont=new Font("Verdana",Font.PLAIN,15);
JTableHeader header=table.getTableHeader();
header.setReorderingAllowed(false);
header.setResizingAllowed(false);
table.setRowSelectionAllowed(true);
header.setFont(headerFont);
table.setFont(tableFont);
jsp=new JScrollPane(table,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
setLayout(new BorderLayout());
add(titleLabel,BorderLayout.NORTH);
add(jsp,BorderLayout.CENTER);
add(button,BorderLayout.SOUTH);
button.addActionListener(this);
}

public void actionPerformed(ActionEvent ae)
{
JFileChooser jfc=new JFileChooser();
jfc.setCurrentDirectory(new File("."));
int selectedOption=jfc.showOpenDialog(this);
if(selectedOption==jfc.APPROVE_OPTION)
{
File selectedFile=jfc.getSelectedFile();
model.add(selectedFile);
}
}

public ArrayList<File> getFiles()
{
return this.model.getFiles();
}

}

class FileUploadViewPanel extends JPanel implements ActionListener,UploadFileListener
{
private JButton uploadButton;
private ArrayList<ProgressPanel> progressPanels;
private JPanel progressPanelsContainer;
private ArrayList<File> files;
private ArrayList<FileUploadThread> fileUploadThreads;
private JScrollPane jsp;

FileUploadViewPanel()
{
uploadButton=new JButton("Upload Files");
uploadButton.addActionListener(this);
setLayout(new BorderLayout());
add(uploadButton,BorderLayout.NORTH);
}

public void actionPerformed(ActionEvent ae)
{
files=fileSelectionPanel.getFiles();
if(files.size()==0)
{
JOptionPane.showMessageDialog(FTClientFrame.this,"No file is selected to upload");
return;
}
progressPanelsContainer=new JPanel();
progressPanelsContainer.setLayout(new GridLayout(files.size(),1));
progressPanels=new ArrayList<>();
ProgressPanel progressPanel;
fileUploadThreads=new ArrayList<>();
FileUploadThread fut;
String id;
for(File file:files)
{
id=UUID.randomUUID().toString();
progressPanel=new ProgressPanel(id,file);
progressPanels.add(progressPanel);
progressPanelsContainer.add(progressPanel);
fut=new FileUploadThread(this,id,host,portNumber,file);
fileUploadThreads.add(fut);
}
jsp=new JScrollPane(progressPanelsContainer,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
add(jsp,BorderLayout.CENTER);
this.revalidate();
this.repaint();
for(FileUploadThread fileUploadThread : fileUploadThreads)
{
fileUploadThread.start();
}
}

public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent)
{
String id=fileUploadEvent.getUploaderId();
File file=fileUploadEvent.getFile();
long numberOfBytesUploaded=fileUploadEvent.getNumberOfBytesUploaded();
for(ProgressPanel progressPanel:progressPanels)
{
if(progressPanel.getId().equals(id))
{
progressPanel.updateProgressBar(numberOfBytesUploaded);
}
}
}

class ProgressPanel extends JPanel
{
private File file;
private String id;
private long fileLength;
private String fileName;
private JLabel titleLabel;
private JProgressBar progressBar;
ProgressPanel(String id,File file)
{
this.id=id;
this.file=file;
fileName=this.file.getAbsolutePath();
titleLabel=new JLabel("Uploading : "+fileName);
fileLength=this.file.length();
progressBar=new JProgressBar();
setLayout(new GridLayout(2,1));
add(titleLabel);
add(progressBar);
}

public String getId()
{
return id;
}

public void updateProgressBar(long bytesUploaded)
{
int percentage;
if(bytesUploaded==fileLength)percentage=100;
else percentage=(int)((bytesUploaded*100)/fileLength);
progressBar.setValue(percentage);
if(percentage==100)
{
titleLabel.setText("Uploaded : "+fileName);
return;
}
}

}//progressPanel ends here

}//inner class ends here
}

class FileUploadThread extends Thread	
{
private UploadFileListener uploadFileListener;
private String id;
private File file;
private String host;
private int portNumber;

FileUploadThread(UploadFileListener uploadFileListener,String id,String host,int portNumber,File file)
{
this.uploadFileListener=uploadFileListener;
this.id=id;
this.host=host;
this.portNumber=portNumber;
this.file=file;
}

public void run()
{
try
{
Socket socket=new Socket(host,portNumber);
String fileName=file.getName();
long fileSize=file.length();
int x=0;
long k=fileSize;
int i=0;
byte []header=new byte[1024];
while(k>0)
{
header[i]=(byte)(k%10);
k=k/10;
i++;
}
header[i]=(byte)',';
i++;
k=fileName.length();
int m=0;
while(m<k)
{
header[i]=(byte)fileName.charAt(m);
m++;
i++;
}
while(i<1024)
{
header[i]=(byte)32;
i++;
}
OutputStream os=socket.getOutputStream();
os.write(header,0,1024);
os.flush();
InputStream is=socket.getInputStream();
byte ack[]=new byte[1];
int readCount=0;
while(true)
{
readCount=is.read(ack);
if(readCount==-1)continue;
break;
}
FileInputStream fis=new FileInputStream(file);
int chunkSize=4096;
byte bytes[]=new byte[chunkSize];
int j=0;
while(j<fileSize)
{
readCount=fis.read(bytes);
os.write(bytes,0,readCount);
os.flush();
j=j+readCount;
long rc=j;
SwingUtilities.invokeLater(()->{
FileUploadEvent fileUploadEvent=new FileUploadEvent();
fileUploadEvent.setUploaderId(id);
fileUploadEvent.setFile(file);
fileUploadEvent.setNumberOfBytesUploaded(rc);
uploadFileListener.fileUploadStatusChanged(fileUploadEvent);
});
}
fis.close();
while(true)
{
readCount=is.read(ack);
if(readCount==0)continue;
break;
}
socket.close();
}catch(Exception e)
{
System.out.println(e);
}
}
}


/*
make header 
write header
read Ack 
write request in chunks 
read ack
close

*/