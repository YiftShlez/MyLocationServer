package com.shlezy.locationserver;

import java.net.InetAddress;
import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

public class Main
{
	private static final int port = 54326;
	private ServerSocket server = null;
	int count = 0;
	boolean running = true;
	Scanner s = null;
	HashMap<String, ClientThread> clients = new HashMap();

	public Main()
	{
		s = new Scanner(System.in);
		try
		{
			server = new ServerSocket(port);
		} catch (IOException ioe)
		{
			System.err.println("Error starting server in port " + port);
			ioe.printStackTrace();
			System.exit(1);
		}
		// For stopping the server just write exit in the command line
		Thread close = new Thread()
		{
			public void run()
			{
				if (s.nextLine().equals("exit"))
				{
					running = false;
					try
					{
						server.close();
					} catch (IOException ioe)
					{
					}
					System.exit(0);
				}
			}
		};
		close.start();
		try
		{
			System.out.println("Started server in address " + InetAddress.getLocalHost() + " in port " + port);
		} catch (UnknownHostException uhe)
		{
			uhe.printStackTrace();
		}

		while (running)

		{
			try
			{
				Socket client = server.accept();
				ClientThread clientThread = new ClientThread(client, count);
				clients.put(clientThread.clientIP, clientThread);
				System.out.println("received client #" + count + " in ip " + clientThread.clientIP);
				clientThread.start();
			} catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
			count++;
		}
		try
		{
			server.close();
		} catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
		System.exit(0);
	}

	public static void main(String[] args)
	{
		new Main();

	}

	class ClientThread extends Thread
	{
		Socket client = null;
		int id = 0;
		String clientIP = "";
		BufferedReader in = null;
		PrintWriter out = null;
		boolean running = false;
		boolean isLocationTaskFinished = false;
		String locationResult;
		Thread listener = new Thread()
		{
			public void run()
			{
				while (running)
				{
					String line = "";
					try
					{
						line = in.readLine();
					} catch (IOException ioe)
					{
						System.err.println("Error listening to client #" + id + " IP " + clientIP);
						ioe.printStackTrace();
						stop();
					}
					System.out.println("Received line from client #" + id + " IP " + clientIP + ": " + line);
					//A result of a check location request
					if (line == null)
						continue;
					if (line.startsWith("locResult:"))
					{
						locationResult = line.substring(10);
						isLocationTaskFinished = true;
					}
					//An option for closing the connection
					else if (line.equals("exit"))
					{
						out.println("Closing connection");
						try
						{
							in.close();
							out.close();
							client.close();
						} catch (IOException ioe)
						{
							System.err.println("Error closing connection from client #" + id + " ip" + clientIP);
							ioe.printStackTrace();
						}
						clients.remove(this);
						running = false;
						ClientThread.this.stop();
					}
					//If the line starts with check: it means the client is checking for the location of other clients
					else if (line.startsWith("check:"))
					{
						String [] params = line.substring(6).split(";");
						//First two parameters are latitude and longitude
						String clientLocation = params [0] + ";" + params [1];
						for (int i = 2; i < params.length; i++)
						{
							//All the other parameters are IP addresses of the client for result checking
							String otherIP = params [i];
							new Thread ()
							{
								public void run ()
								{
									out.println ("locResult:" + otherIP + ";" + clients.get(otherIP).checkLocation(clientLocation));
								}
							}.start();
						}
					}
				}
			}
		};
		/**
		 * Constructor
		 * @param client the socket of the client, received in ServerSocket.accept()
		 * @param id the client ID
		 */
		public ClientThread(Socket client, int id)
		{
			this.client = client;
			this.id = id;
			clientIP = client.getInetAddress().getHostAddress();
		}
		
		/**
		 * A function for checking the client's location
		 * @param location a location to measure the distance from
		 * @return the location if the distance from the location is less than 300 meters, or error message
		 */
		public String checkLocation(String location)
		{
			out.println("check:" + location);
			while (!isLocationTaskFinished)
				;
			isLocationTaskFinished = false;
			return locationResult;
		}

		public void run()
		{
			try
			{
				out = new PrintWriter(client.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			} catch (IOException ioe)
			{
				System.err.println("Error connecting to client's io streams. IP: " + clientIP + " ID: " + id);
				ioe.printStackTrace();
				stop();
			}
			running = true;
			listener.start();
		}
	}

}
