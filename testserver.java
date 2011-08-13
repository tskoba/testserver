import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public class testserver extends Thread implements KeyListener, FocusListener {
  final static int bufsize = 256;
  private class testserver_reader extends Thread {
    private BufferedReader reader;
    private testserver notifier;
    public testserver_reader(InputStream is, testserver n) {
      reader = new BufferedReader(new InputStreamReader(is));
      notifier = n;
    }

    public void run() {
      try {
	String s;
	while((s = reader.readLine()) != null) {
	  notifier.reader_textarea.append(s+'\n');
	  notifier.rw_textarea.append(s+'\n');
	}
      } catch(SocketException e) {
	System.err.println(e);
      } catch(IOException e) {
	System.err.println(e);
      } finally {
	try {
	  reader.close();
	} catch(IOException e) {
	  System.err.println(e);
	}
	synchronized(notifier) { notifier.notify(); }
	System.out.println("Exiting testserver_reader.run()");
      }
    }
  }

  private class testserver_writer extends Thread {
    private PrintWriter writer;
    private testserver notifier;
    public testserver_writer(OutputStream os, testserver n) {
      writer = new PrintWriter(new OutputStreamWriter(os), true);
      notifier = n;
    }

    public void print(String s) {
      writer.print(s);
      writer.flush();
      notifier.rw_textarea.append(s);
    }

    public void run() {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      try {
	while(true) {
	  if(reader.ready()) {
	    char[] str = new char[bufsize];
	    int nread = reader.read(str, 0, bufsize);
	    if(nread == -1)
	      break;
	    String s = new String(str, 0, nread);
	    print(s);
	  } else {
	    sleep(1);
	  }
	}
      } catch(IOException e) {
	System.err.println(e);
      } catch(InterruptedException e) {
      } finally {
	writer.close();
	synchronized(notifier) { notifier.notify(); }
	System.out.println("Exiting testserver_writer.run()");
      }
    }
  }

  private Socket socket;
  private testserver_reader reader;
  private testserver_writer writer;
  private JFrame frame;
  private JTextArea reader_textarea, writer_textarea, rw_textarea;
  private StringBuffer buffer = new StringBuffer();
  private testserver(Socket s) throws IOException {
    socket = s;
    reader = new testserver_reader(s.getInputStream(), this);
    writer = new testserver_writer(s.getOutputStream(), this);
    make_frame();
  }

  private void make_frame() {
    frame = new JFrame(socket.toString());
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    Container c = frame.getContentPane();
    c.setLayout(new GridLayout(1,2));

    JPanel jp12 = new JPanel();
    c.add(jp12);
    jp12.setLayout(new GridLayout(2,1));

    JPanel jp1 = new JPanel();
    jp1.setBorder(BorderFactory.createCompoundBorder(
	     BorderFactory.createTitledBorder("Reader"),
	     BorderFactory.createEmptyBorder(5,5,5,5)));
    jp1.setLayout(new BorderLayout());
    reader_textarea = new JTextArea();
    reader_textarea.setEditable(false);
    System.out.println(reader_textarea.getKeymap());
    reader_textarea.addKeyListener(this);
    jp1.add(new JScrollPane(reader_textarea));
    jp12.add(jp1);

    JPanel jp2 = new JPanel();
    jp2.setBorder(BorderFactory.createCompoundBorder(
	     BorderFactory.createTitledBorder("Writer"),
	     BorderFactory.createEmptyBorder(5,5,5,5)));
    jp2.setLayout(new BorderLayout());
    writer_textarea = new JTextArea();
    writer_textarea.setEditable(false);
    writer_textarea.addKeyListener(this);
    jp2.add(new JScrollPane(writer_textarea));
    jp12.add(jp2);

    JPanel jp3 = new JPanel();
    jp3.setBorder(BorderFactory.createCompoundBorder(
	     BorderFactory.createTitledBorder("Log"),
	     BorderFactory.createEmptyBorder(5,5,5,5)));
    jp3.setLayout(new BorderLayout());
    rw_textarea = new JTextArea();
    rw_textarea.setEditable(false);
    rw_textarea.addKeyListener(this);
    jp3.add(new JScrollPane(rw_textarea));
    c.add(jp3);

    frame.addKeyListener(this);

    //    frame.pack();
    frame.setSize(300, 200);
    frame.setVisible(true);
  }

  public void keyPressed(KeyEvent e) { }

  public void keyReleased(KeyEvent e) { }

  public void keyTyped(KeyEvent e)
  {
    char c = e.getKeyChar();
    if(c == KeyEvent.VK_ENTER) {
      writer_textarea.append(new Character('\n').toString());
      buffer.append('\n');
      writer.print(new String(buffer));
      buffer = new StringBuffer();
    } else if(c == KeyEvent.VK_BACK_SPACE) {
      int len = buffer.length();
      if(0 < len) {
	String s = writer_textarea.getText();
	writer_textarea.setText(s.substring(0, s.length()-1));
	buffer.deleteCharAt(len-1);
      }
    } else {
      writer_textarea.append(new Character(c).toString());
      buffer.append(c);
    }
    System.out.println(e);
  }

  public void focusGained(FocusEvent e)
  {/*System.out.println(e);*/}
  public void focusLost(FocusEvent e)
  {/*System.out.println(e);*/}

  synchronized public void run() {
    writer.start();
    reader.start();
    try {
      System.out.println("wait()");
      wait();
    } catch(InterruptedException e) {
      System.err.println(e);
    }
    if(writer.isAlive()) {
      System.out.println("writer.interrupt()");
      writer.interrupt();
    }
    if(reader.isAlive()) {
      System.out.println("reader.interrupt()");
      reader.interrupt();
    }
    try {
      socket.close();
    } catch(IOException e) {
      System.err.println(e);
    }
    System.out.println("Exiting testserver.run()");
  }

  public static void main(String[] args) throws IOException {
    int port = Integer.parseInt(args[0]);
    ServerSocket server_socket = new ServerSocket(port);
    while(true) {
      Socket s = server_socket.accept();
      testserver t = new testserver(s);
      t.start();
    }
  }
}
