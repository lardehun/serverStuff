package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

import commonenums.Command;
import mp3_joiner.MP3Joiner;

public class JavanymousServer
{

	@SuppressWarnings ("unchecked")
	public JavanymousServer (int port)
	{
		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		ObjectInputStream ois = null;
		ObjectOutputStream oos = null;

			try
			{
				serverSocket = new ServerSocket(port);
				System.out.println("Waiting for client...");
				clientSocket = serverSocket.accept();
				oos = new ObjectOutputStream(clientSocket.getOutputStream());
				ois = new ObjectInputStream(clientSocket.getInputStream());

				boolean serverRunning = true;
				while (serverRunning) {
					
					// reading the command from the client
					Object objectFromInput = null;
					try {
						System.out.println("Waiting for command.");
						objectFromInput = ois.readObject();
						System.out.println("Command arrived.");
					} catch (java.net.SocketException e) {
						e.printStackTrace();
						System.out.println("Client ShutDown.");
						break;}
					
					if (objectFromInput.equals(Command.JOIN))
					{
						// getting the file list from the client (what m3u contained)
						List< File > listFiles = (List< File >) ois.readObject();
						List< File > serverFiles = new ArrayList< File >();
						// getting the mp3 content and saving them on the server side
						String saveDir = dirCreation();
						for (File file : listFiles)
						{
							File destination = new File(saveDir + "//" +file.getName());
							serverFiles.add(destination);
							saveFile(clientSocket, destination, file.length());
						}
						// joining the new file
						File finalFile = new File(saveDir + "//newJoined.mp3");
						MP3Joiner.joinFiles(serverFiles, finalFile);
						
						// sending back the joined mp3 file
						oos.writeObject(finalFile.length());
						sendFile(finalFile, clientSocket);
						oos.flush();
						continue;
					}
					
					else if (objectFromInput.equals(Command.SORT))
					{
						// TODO
					}
					else if (objectFromInput.equals(Command.EXIT))
					{
						// exit from the server.
						System.out.println("SERVER SHUTDOWN!");
						clientSocket.close();
						serverSocket.close();
						serverRunning = false;
						
					}
				}
					
			}
			
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
		

	@SuppressWarnings("unused")
	private void saveFile(Socket clientSock, File file, long fileSize) throws IOException
	{
		DataInputStream dis = new DataInputStream(clientSock.getInputStream());
		FileOutputStream fos = new FileOutputStream(file);
		byte [ ] buffer = new byte [ 4096 ];

		int read = 0;
		int totalRead = 0;
		long remaining = fileSize;
		double percentage;
		while ((read = dis.read(buffer, 0, Math.min(buffer.length, (int) remaining))) > 0){
			totalRead += read;
			remaining -= read;
//			System.out.println("read " + totalRead + " bytes.");
//			percentage = ((double)totalRead)/(double)(totalRead+remaining)*100;
//			System.out.println(String.format("%.1f", percentage) + " %");
			fos.write(buffer, 0, read);
			if (remaining <= 0) {
				System.out.println("One mp3 saved on server.");
			}
		}
		fos.close();
	}

	public void sendFile(File file, Socket s) throws IOException
	{
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		FileInputStream fis = new FileInputStream(file);
		byte [ ] buffer = new byte [ 4096 ];
		int read;
		while ((read = fis.read(buffer)) > 0){
			dos.write(buffer, 0, read);
			dos.flush();
		}
		fis.close();
	}
	
	public String dirCreation() {
		FileSystemView filesys = FileSystemView.getFileSystemView();
		@SuppressWarnings("unused")
		File[] roots = filesys.getRoots();
		String home = filesys.getHomeDirectory().getAbsolutePath();
		File newDir = new File(home +"//ServerSaves");
		if (!newDir.exists()) {
			newDir.mkdir();
		}
		return newDir.getAbsolutePath();
	}

	
	public static void main(String [ ] args)
	{
		new JavanymousServer(10022);
	}
}
