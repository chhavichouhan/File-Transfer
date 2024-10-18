import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

class RequestProcessor extends Thread
{
private Socket socket;
private FTServerFrame fsf;
private String id;

RequestProcessor(Socket socket,String id,FTServerFrame fsf)
{
this.socket=socket;
this.id=id;
this.fsf=fsf;
start();
}

public void run()
{
try
{
SwingUtilities.invokeLater(()->{
fsf.updateLog("Client ready with host id : "+id);
});
OutputStream os=socket.getOutputStream();
InputStream is=socket.getInputStream();
byte [] header=new byte[1024];
byte [] tmp=new byte[1024];
int readCount=0;
int j=0;
int i=0;
while(j<1024)
{
readCount=is.read(tmp);
if(readCount==-1)continue;
for(int k=0;k<readCount;k++)
{
header[i]=tmp[k];
i++;
}
j=j+readCount;
}
i=0;
j=1;
long fileLength=0;
while(header[i]!=',')
{
fileLength=fileLength+(header[i]*j);
j=j*10;
i++;
}
i++;
StringBuffer sb=new StringBuffer();
while(i<1024)
{
sb.append((char)header[i]);
i++;
}
String fileName=sb.toString().trim();
SwingUtilities.invokeLater(()->{
fsf.updateLog("Reciving files of host id : "+id+" and name "+fileName);
});
byte[] ack=new byte[1];
ack[0]=1;
os.write(ack,0,1);
os.flush();
File file=new File("upload"+File.separator+fileName);
FileOutputStream fos=new FileOutputStream(file);
int chunkSize=4096;
byte bytes[]=new byte[chunkSize];
j=0;
readCount=0;
while(j<fileLength)
{
readCount=is.read(bytes);
fos.write(bytes,0,readCount);
j=j+readCount;
}
fos.close();
ack[0]=1;
os.write(ack,0,1);
os.flush();
SwingUtilities.invokeLater(()->{
fsf.updateLog("File saved "+file.getAbsolutePath());
});
socket.close();
}catch(Exception e)
{
System.out.println(e);
}
}
}

class FTServer2 extends Thread
{
private FTServerFrame fsf;
private ServerSocket serverSocket;
FTServer2(FTServerFrame fsf)
{
this.fsf=fsf;
}

public void run()
{
try
{
serverSocket=new ServerSocket(5500);
startListening();
}catch(Exception e)
{
System.out.println(e);
}
}

public void shutDown()
{
try
{
serverSocket.close();
}catch(Exception e)
{
System.out.println(e);
}
}
public void startListening()
{
try
{
Socket socket;
RequestProcessor requestProcessor;
while(true)
{
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fsf.updateLog("Server is ready to accept request at port 5500");
}
});
socket=serverSocket.accept();
requestProcessor=new RequestProcessor(socket,UUID.randomUUID().toString(),fsf);
}
}
catch(Exception e)
{
System.out.println("Server socket is closed");
System.out.println(e);
}
}

}

class FTServerFrame extends JFrame implements ActionListener
{
private FTServer2 server;
private JButton button;
private JTextArea textArea;
private Container container;
private JScrollPane jsp;
private boolean serverState=false;

FTServerFrame()
{
button=new JButton("Start");
textArea=new JTextArea();
jsp=new JScrollPane(textArea,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
container=getContentPane();
container.setLayout(new BorderLayout());
container.add(button,BorderLayout.SOUTH);
container.add(jsp,BorderLayout.CENTER);
button.addActionListener(this);
setLocation(100,100);
setSize(400,400);
setVisible(true);
}

public void updateLog(String message)
{
textArea.append(message+"\n");
}

public void actionPerformed(ActionEvent ae)
{
try
{
if(serverState==false)
{
server=new FTServer2(this);
server.start();
serverState=true;
button.setText("Stop");
}
else
{
server.shutDown();
serverState=false;
button.setText("Start");
textArea.append("Server stopped\n");
}
}catch(Exception e)
{
System.out.println(e);
}
}

public static void main(String[]gg)
{
FTServerFrame ftServerFrame=new FTServerFrame();
}

}












