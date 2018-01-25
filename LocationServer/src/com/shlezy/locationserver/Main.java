package com.shlezy.locationserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;

public class Main
{
	private static final int port = 54326;
	private ServerSocket server = null;
	int count = 0;
	boolean running = true;
	Scanner s = null;
	String location = "32.2260517,35.1627933";

	public Main()
	{
		s = new Scanner(System.in);
		System.out.println("Enter location in this format:\nlatitude,langitude\nor default");
		String input = s.nextLine();
		if (!input.equalsIgnoreCase("default"))
		{
			location = input;
		}
		System.out.println("Location chosen: " + location);
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
		System.out
				.println("Started server in address " + server.getInetAddress().getHostAddress() + " in port " + port);
		while (running)

		{
			try
			{
				Socket client = server.accept();
				System.out.println("received client #" + count);
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				PrintWriter out = new PrintWriter(client.getOutputStream(), true);
				out.println("location;" + location);
				String line = in.readLine();
				System.out.println("Received line from client #" + count + ": " + line);
				out.close();
				in.close();
				client.close();
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

}
