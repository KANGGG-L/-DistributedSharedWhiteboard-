package client;

import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import javax.swing.*;

import org.javatuples.Triplet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import remote.*;


public class WhiteboardClient extends UnicastRemoteObject implements IRemoteWhiteboardClient {
    // Startup
    private String userName;
	private IRemoteWhiteboard whiteboard;

    // Drawing
    private static JFrame frame;
    protected JPanel drawingPanel;

	protected int lastX, lastY;

    private Map<Color, String> colorMap; 
	private Map<String, Color> colorRevMap; 
    protected Color currColor = Color.BLACK;
    protected JLabel colorLabel = new JLabel("Current Color: " + "BLACK");

	protected String currFunction = "Free Draw";
	protected String eraserSize = "Small";
    protected String defaultData = "";

    private ArrayList<Triplet<Shape, Color, String>> drawingTuples = new ArrayList<>();

    protected int unrepaintCount = 0; // Refresh counter

    // For Restore Default state
    protected JButton blackButton;
    protected JRadioButton freeDrawButton;
    protected JComboBox eraseBox;
    protected JTextField chatField;

    // Chat
	private JTextArea chatHistoryArea;
    protected JTextArea inputTextArea = new JTextArea();

    // Active User
    protected ArrayList<String> userNameList = new ArrayList<>();

    private Boolean isManager = false;
    private Boolean isAccepted = false;

    // Manger Only
    protected String filePath = "";
    protected JLabel notificationLabel = new JLabel("Someone Wants To Join the Whiteboard, Please Check At 'Waiting Room' of Manage Menu");
    protected ArrayList<String> chatHistory = new ArrayList<>();

    protected Boolean isProactiveClose = true;
    

    public WhiteboardClient(String userName, IRemoteWhiteboard whiteboard) throws RemoteException {
        this.userName = userName;
        this.whiteboard = whiteboard;
        isManager = whiteboard.registerClient(this);

        if (!isManager) {
            try {
                while (whiteboard.isInWating(userName)) {
                    // Keep Asking if it is still in waiting room
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception ex) {
                        System.err.println("HERE");
                    } 
                    
                }
            } catch (Exception e) {
                if (e instanceof NullPointerException) {
                    System.out.println("User Has Been Removed From Waiting Room");
                } else {
                    System.err.println(e.getMessage());
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
               if (!isManager && !isAccepted) {
                    try {
                        whiteboard.removeFromWaitingPool(userName);
                        System.out.println("Sucessfully Shut Down GUI");
                    } catch (Exception e) {
                        if (e instanceof RemoteException) {
                            System.err.println("Remote Exception Is Raised When Closing GUI");
                        } else if (e instanceof NullPointerException) {
                            System.err.println("User Is Removing From Waiting Room");
                        } else {
                            System.err.println(e.getMessage());
                        }
                    }
               }
                
            }
        });


        if (isManager || isAccepted) {
            // Set up frame
            new ColorMap();
            colorMap = ColorMap.getColorMap();
            colorRevMap = ColorMap.getColorRevMap();
            createAndShowGUI();

            synchronizeChat(whiteboard.getChatHistory());
            synchronizeDrawing(whiteboard.getDrawingTuples());
        } else {
            System.out.println("Manager Has Declined Your Request");
            System.exit(0);
        }
		
        
    }

    @Override
    public void setAccpet() throws RemoteException {
        isAccepted = true;
    }

    @Override
    public String getUserName() throws RemoteException {
        return userName;
    }

    public void synchronizeChat(ArrayList<String> newChatHistory) {

        chatHistoryArea.setText("");
        for (String message: newChatHistory) {
            chatHistoryArea.append(message);
        }
    }

    @Override
	public synchronized void receiveMessage(ArrayList<String> newChatHistory) throws RemoteException {
        synchronizeChat(newChatHistory);
	}

    public void synchronizeDrawing(ArrayList<Triplet<Shape, Color, String>> newDrawingTuples) {
        drawingTuples = newDrawingTuples;
        drawingPanel.repaint();
    }


    @Override
	public void receiveDrawing(ArrayList<Triplet<Shape, Color, String>> newDrawingTuples) throws RemoteException {
        synchronizeDrawing(newDrawingTuples);
	}


    @Override
    public void remindManager(Boolean flag) throws RemoteException {
        if (isManager) {
            notificationLabel.setVisible(flag);
        }
    }

    

    public void broadcastToWhiteboard (Triplet<Shape, Color, String> drawingTuple) {
        try {
            whiteboard.broadcastDrawing(drawingTuple);
        } catch (RemoteException ex) {
            System.err.println("Remote Exception Is Raised When Broadcast To Whiteboard");
            System.exit(1);
        }
    }

    ///////////////////////////////////////// 
    /////////     Integrate GUI      ////////
    /////////////////////////////////////////
	private void createAndShowGUI() {
        frame = new JFrame();
		frame.setTitle("Drawing Application");
        frame.setBounds(100, 100, 800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		inputTextArea.setEditable(true);
		inputTextArea.setBorder(BorderFactory.createLineBorder(Color.black));
		inputTextArea.setPreferredSize(new Dimension(200, 20));
		inputTextArea.setVisible(true);


        JScrollPane scrollPane = newScrollPane();
        chatField = newChatField();
        JButton sendButton = newSendButton(chatField);
        JPanel chatPanel = newChatJPanel(scrollPane, chatField, sendButton);

        JPanel drawingPanel = newDrawingPanel();
        notificationLabel.setHorizontalAlignment(SwingConstants.CENTER);
		notificationLabel.setFont(new Font("Verdana", Font.PLAIN, 10));
		notificationLabel.setForeground(Color.RED);
		notificationLabel.setVisible(false);
		drawingPanel.add(notificationLabel, BorderLayout.NORTH);

        JPanel colorPanel = newColorPanel();
        JPanel entireColorSection = newEntireColorSection(colorPanel);

        JPanel functionPanel = newFunctPanel(drawingPanel, sendButton);
        JPanel drawingToolPanel = newDrawingToolPanel(entireColorSection, functionPanel);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        menuBar.add(newMenue());

		frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(drawingPanel, BorderLayout.CENTER);
        frame.getContentPane().add(drawingToolPanel, BorderLayout.WEST);
		frame.getContentPane().add(chatPanel, BorderLayout.EAST);
		frame.setVisible(true);

        ////////////////////////////
        ////    Shut Down GUI   ////
        ////////////////////////////

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
               
                if (isProactiveClose) {
                    try {
                        whiteboard.deregisterClient(WhiteboardClient.this);
                        System.out.println("Sucessfully Shut Down GUI");
                    } catch (RemoteException e) {
                        System.err.println("Remote Exception Is Raised When Closing GUI");
                    }
                }  
                
                
            }
        });


        
        
        
    }


    @Override
    public void kickUser(int mode) throws RemoteException {
        isProactiveClose = false;
        if (mode == 0) {
            System.out.println("Manager Has Terminate Whiteboard");
        } else {
            System.out.println("You Have Been Kicked Out By Manager");
        }

        System.exit(0);
    }

    ///////////////////////////////////////// 
    //////////      Drawing Panel    ////////
    /////////////////////////////////////////
    private JPanel newDrawingPanel() {
        drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;

				for (int i = 0; i < drawingTuples.size(); i++) {
					
                    Shape currShape = drawingTuples.get(i).getValue0();
                    Color currColor = drawingTuples.get(i).getValue1();
                    String currFunctionData = drawingTuples.get(i).getValue2();

                    g2d.setColor(currColor); 
                    
					
					if (currFunctionData.length() > 0)  {
						if (currFunctionData.charAt(0) == '!')  {
							// Function text
							
							Rectangle2D.Double temp = (Rectangle2D.Double) currShape;

							g2d.drawString(currFunctionData.substring(1), (int) temp.getX(), (int) temp.getY());
                            continue;
							
						} else {
						
							String[] parts = currFunctionData.split("!");

							int[] intParts = new int[parts.length];
							for (int j = 0; j < parts.length; j++) {
								intParts[j] = Integer.parseInt(parts[j]); 
							}

							g2d.fillRect(intParts[0], intParts[1], intParts[2], intParts[2]);

							
						}

					} 

                    
					g2d.draw(currShape);

                
					
				}
				
            }
        };
		drawingPanel.setLayout(new BorderLayout());
		
        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();

				if (currFunction.equals("Free Draw") || currFunction.equals("Erase")) {
                    broadcastToWhiteboard(Triplet.with(new Line2D.Double(lastX, lastY, lastX, lastY), currColor, defaultData));
				}
            }

			@Override
            public void mouseReleased(MouseEvent e) {
				if (!currFunction.equals("Free Draw") && !currFunction.equals("Erase")) {
					int currX = e.getX();
                	int currY = e.getY();

					boolean emptyInput = false;

                    Shape currShape = new Line2D.Double();
					switch (currFunction) {
						case "Line":
							currShape = new Line2D.Double(lastX, lastY, currX, currY);
							break;
						case "Circle":
							int radius = (int) Math.sqrt(Math.pow(lastX - currX, 2) + Math.pow(lastY - currY, 2));
							currShape = new Ellipse2D.Double(lastX - radius, lastY - radius, 2 * radius, 2 * radius);
							break;
						case "Oval":
                            currShape = new Ellipse2D.Double(lastX, lastY, Math.abs(currX - lastX), Math.abs(currY - lastY));
							break;
						case "Rectangle":
                            currShape = new Rectangle2D.Double(lastX, lastY, Math.abs(currX - lastX), Math.abs(currY - lastY));
							break;
						case "Text":
							if (!inputTextArea.getText().trim().isEmpty()) {
                                broadcastToWhiteboard(Triplet.with(new Rectangle2D.Double(currX, currY, 0, 0), currColor, "!" + inputTextArea.getText()));
								inputTextArea.setText("");
							}
							emptyInput = true;
							
							break;

							
					}

					if (!emptyInput) {
                        broadcastToWhiteboard(Triplet.with(currShape, currColor, defaultData));
					}

					lastX = currX;
					lastY = currY;
			
				}
				
				drawingPanel.repaint();
            }

			
        });

	

        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
				if (currFunction.equals("Free Draw")) {

                    broadcastToWhiteboard(Triplet.with(new Line2D.Double(lastX, lastY, e.getX(), e.getY()), currColor, defaultData));

					lastX = e.getX();
					lastY = e.getY();

					drawingPanel.repaint();
				} else if (currFunction.equals("Erase")) {
					int size = 0;

					switch (eraserSize) {
						case "Small":
							size = 5;
							break;
						case "Medium":
							size = 10;
							break;
						case "Large":
							size = 15;
							break;
					}
                    
                    broadcastToWhiteboard(Triplet.with(new Line2D.Double(lastX, lastY, e.getX(), e.getY()), Color.WHITE, Integer.toString(e.getX() - size) + "!" + Integer.toString(e.getY() - size) + "!" + Integer.toString(size)));
                   

					lastX = e.getX();
					lastY = e.getY();

					drawingPanel.repaint();
				} else {

                    // Only for local graph shape tracking
					if (unrepaintCount == 10) {
						drawingPanel.repaint();
						unrepaintCount = 0;
					}
					Graphics g = drawingPanel.getGraphics();
					g.setColor(currColor);
					switch (currFunction) {
						case "Line":
							g.drawLine(lastX, lastY, e.getX(), e.getY());
							break;
						case "Circle":
							int radius = (int) Math.sqrt(Math.pow(e.getX() - lastX, 2) + Math.pow(e.getY() - lastY, 2));
							g.drawOval(lastX - radius, lastY - radius, 2 * radius, 2 * radius);
							break;
						case "Oval":
							g.drawOval(lastX, lastY, e.getX() - lastX, e.getY() - lastY);
							break;
						case "Rectangle":
							g.drawRect(lastX, lastY, e.getX() - lastX, e.getY() - lastY);
							break;
                	}

					unrepaintCount += 1;
				}

            }
        });

        return drawingPanel;

    }




    /////////////////////////////////////// 
    ////////       Color Panel     ////////
    ///////////////////////////////////////

    private JPanel newEntireColorSection(JPanel colorPanel) {

		colorLabel.setHorizontalAlignment(SwingConstants.CENTER);
		colorLabel.setFont(new Font("Verdana", Font.PLAIN, 10));
		colorLabel.setVisible(true);

		
		JPanel entireColorSection = new JPanel();
		entireColorSection.setLayout(new BorderLayout());
		entireColorSection.setPreferredSize(new Dimension(150, 250));
		entireColorSection.add(colorPanel, BorderLayout.NORTH);
		entireColorSection.add(colorLabel, BorderLayout.CENTER);

        return entireColorSection;
    }
		
    private JPanel newColorPanel() {
        JPanel colorPanel = new JPanel();
        colorPanel.setLayout(new GridLayout(3, 4));
        colorPanel.setPreferredSize(new Dimension(150, 200));

        blackButton = createColorButton(Color.BLACK);
        JButton blueButton = createColorButton(Color.BLUE);
        JButton cyanButton = createColorButton(Color.CYAN);
        JButton darkGrayButton = createColorButton(Color.DARK_GRAY);
        JButton grayButton = createColorButton(Color.GRAY);
        JButton greenButtonn = createColorButton(Color.GREEN);
        JButton lightGrayButton = createColorButton(Color.LIGHT_GRAY);
        JButton magentaButton = createColorButton(Color.MAGENTA);
        JButton orangeButtonn = createColorButton(Color.ORANGE);
        JButton redButton = createColorButton(Color.RED);
        JButton pinkButtonn = createColorButton(Color.PINK);
        JButton yellowButtonn = createColorButton(Color.YELLOW);

        colorPanel.add(blackButton);
        colorPanel.add(blueButton);
        colorPanel.add(cyanButton);
        colorPanel.add(darkGrayButton);
        colorPanel.add(grayButton);
        colorPanel.add(greenButtonn);
        colorPanel.add(lightGrayButton);
        colorPanel.add(magentaButton);
        colorPanel.add(orangeButtonn);
        colorPanel.add(redButton);
        colorPanel.add(pinkButtonn);
        colorPanel.add(yellowButtonn);

        return colorPanel;
    }

    private JButton createColorButton(Color color) {
        JButton button = new JButton();
        button.setBackground(color);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(50, 50));
		button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currColor = color;
				colorLabel.setText("Current Color: " + colorMap.get(currColor));
            }
        });
		return button;
	}
    

    /////////////////////////////////////// 
    ////////     Function Panel    ////////
    ///////////////////////////////////////
    private JPanel newFunctPanel(JPanel drawingPanel, JButton blackButton) {
        JPanel functionPanel = new JPanel();
        functionPanel.setLayout(new GridLayout(7, 1));
		functionPanel.setPreferredSize(new Dimension(150, 150));

		freeDrawButton = createRadioButton("Free Draw");
		JRadioButton textButton = createRadioButton("Text");
		JRadioButton lineButton = createRadioButton("Line");
        JRadioButton circleButton = createRadioButton("Circle");
        JRadioButton ovalButton = createRadioButton("Oval");
        JRadioButton rectangleButton = createRadioButton("Rectangle");

		eraseBox = new JComboBox();
		eraseBox.setModel(new DefaultComboBoxModel(new String[] {"Small", "Medium", "Large"}));
		eraseBox.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		    	eraserSize = ((String) eraseBox.getSelectedItem());	
		    }
		});


		JRadioButton eraseButton = createRadioButton("Erase");
		JPanel erasePanel = new JPanel();
		erasePanel.setLayout(new GridLayout(1, 2));
		erasePanel.setPreferredSize(new Dimension(150, 10));
		erasePanel.add(eraseButton);
		erasePanel.add(eraseBox);

		
		ButtonGroup group = new ButtonGroup();
		group.add(freeDrawButton);
		group.add(eraseButton);
		group.add(lineButton);
		group.add(circleButton);
		group.add(ovalButton);
		group.add(rectangleButton);
		group.add(textButton);

		freeDrawButton.setSelected(true);
	
		functionPanel.add(freeDrawButton);
		functionPanel.add(erasePanel);
		functionPanel.add(lineButton);
        functionPanel.add(circleButton);
        functionPanel.add(ovalButton);
        functionPanel.add(rectangleButton);
		functionPanel.add(textButton);

		freeDrawButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				restoreFromErase(blackButton);
				restoreFromText(drawingPanel);
                currFunction = "Free Draw";
            }
        });

		eraseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				restoreFromText(drawingPanel);
                currFunction = "Erase";
				currColor = Color.WHITE;

				colorLabel.setVisible(false);
            } 
        });

		lineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				restoreFromErase(blackButton);
				restoreFromText(drawingPanel);
                currFunction = "Line";
            }
        });

        circleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				restoreFromErase(blackButton);
				restoreFromText(drawingPanel);
                currFunction = "Circle";
            }
        });

        ovalButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				restoreFromErase(blackButton);
				restoreFromText(drawingPanel);
                currFunction = "Oval";
            }
        });

        rectangleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				restoreFromErase(blackButton);
				restoreFromText(drawingPanel);
                currFunction = "Rectangle";
            }
        });

		textButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

				restoreFromErase(blackButton);
                currFunction = "Text";
				
				drawingPanel.add(inputTextArea, BorderLayout.NORTH);
				drawingPanel.revalidate();
        		drawingPanel.repaint();
				
            }
        });

        return functionPanel;
    }

    private JRadioButton createRadioButton(String text) {
        JRadioButton radioButton = new JRadioButton(text);
        radioButton.setActionCommand(text);
        return radioButton;
    }

    private void restoreFromErase(JButton blackButton) {
		if (currFunction.equals("Erase")) {
			currColor = Color.BLACK;
			blackButton.setSelected(true);
			colorLabel.setText("Current Color: " + colorMap.get(currColor));
			colorLabel.setVisible(true);
		} 

	}

	private void restoreFromText(JPanel drawingPanel) {
		if (currFunction.equals("Text")) {
			drawingPanel.remove(inputTextArea);
			drawingPanel.revalidate();
			drawingPanel.repaint();
		}
	}

    private JPanel newDrawingToolPanel(JPanel entireColorSection, JPanel functionPanel) {
        JPanel drawingToolPanel = new JPanel();
        drawingToolPanel.setLayout(new BorderLayout());
		drawingToolPanel.add(entireColorSection, BorderLayout.NORTH);
		drawingToolPanel.add(functionPanel, BorderLayout.SOUTH);

        return drawingToolPanel;
    }

    /////////////////////////////////////// 
    ////////      Chat Section     ////////
    ///////////////////////////////////////
    private JPanel newChatJPanel(JScrollPane scrollPane, JTextField chatField, JButton sendButton) {
        

        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
		chatPanel.add(scrollPane, BorderLayout.NORTH);
		chatPanel.add(chatField, BorderLayout.CENTER);
		chatPanel.add(sendButton, BorderLayout.SOUTH);

        return chatPanel;
    }

    private JScrollPane newScrollPane() {
        chatHistoryArea = new JTextArea();
		chatHistoryArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(chatHistoryArea);
        scrollPane.setPreferredSize(new Dimension(200, 350));
		scrollPane.setBorder(BorderFactory.createLineBorder(Color.black));

        return scrollPane;
    }



    private JTextField newChatField() {
        JTextField chatField = new JTextField();
		chatField.setBorder(BorderFactory.createLineBorder(Color.black));
        return chatField;
    }

    private JButton newSendButton(JTextField chatField) {
        JButton sendButton = new JButton("Send");
		sendButton.setPreferredSize(new Dimension(200, 30));
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = chatField.getText();

				if (!message.trim().isEmpty()) {

					String msg = '"' + userName + '"' + ": " + message.trim() + "\n";

                    try {
                        whiteboard.broadcastMessage(msg);
                    } catch (RemoteException ex) {
                        System.err.println("Remote Exception Is Raised When Sending Message");
                        System.exit(1);
                    }
                    chatField.setText("");
                }
            }
        });

        return sendButton;
    }

    /////////////////////////////////////// 
    ////////         Menu         ////////
    ///////////////////////////////////////
    public JMenu newMenue() {

		JMenu menu = new JMenu("Menu");

        JMenu manageMenu = new JMenu("Manage");
        JMenuItem usersMenuItem = new JMenuItem("Active Users");
        manageMenu.add(usersMenuItem);

        usersMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                JFrame activeUserFrame = new JFrame();
                activeUserFrame.setBounds(200, 150, 200, 200);
                activeUserFrame.setTitle("Active Users");
                
               
                
                try {
                    ArrayList<String> userNames = whiteboard.getActiveUserNames();


                    JPanel managePanel = new JPanel();
                    managePanel.setLayout(new GridLayout(1, 2));
                    managePanel.setPreferredSize(new Dimension(150, 150));
    
                    JPanel nameLebelPanel = new JPanel();
                    nameLebelPanel.setLayout(new GridLayout(userNameList.size(),1));
                    nameLebelPanel.setPreferredSize(new Dimension(50, 50));
    
                    JPanel buttonPanel = new JPanel();
                    buttonPanel.setLayout(new GridLayout(userNameList.size(),1));
                    buttonPanel.setPreferredSize(new Dimension(50, 50));
                   
                    Boolean flag = true;
                    

                    for (String name: userNames) {
                        JLabel nameLabel = new JLabel(name);

                        nameLabel.setFont(new Font("Verdana", Font.BOLD, 20));
                        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        nameLabel.setVerticalAlignment(SwingConstants.CENTER);
                        nameLebelPanel.add(nameLabel);


                        if (isManager) {
                            JButton button = createManageButton(1, name);

                            if (flag) {
                                button.setVisible(false); // Hide the button for manager
                                flag = false;
                            }
                            
                            buttonPanel.add(button);
                        }
                        
		            }

                    managePanel.add(nameLebelPanel);
                    managePanel.add(buttonPanel);
    
                    activeUserFrame.add(managePanel, BorderLayout.CENTER);
                    activeUserFrame.setVisible(true);

                } catch (RemoteException e1) {
                    System.err.println("Remote Exception Is Raised When Getting Active User List");
                    System.exit(1);
                }

                

               
            }
        });


        if (isManager) {
            JMenu fileMenu = new JMenu("File");
            JMenuItem newMenuItem = new JMenuItem("New");
            JMenuItem openMenuItem = new JMenuItem("Open");
            JMenuItem saveMenuItem = new JMenuItem("Save");
            JMenuItem saveAsMenuItem = new JMenuItem("Save As");
            JMenuItem closeMenuItem = new JMenuItem("Close");

            fileMenu.add(newMenuItem);
            fileMenu.add(openMenuItem);
            fileMenu.add(saveMenuItem);
            fileMenu.add(saveAsMenuItem);
            fileMenu.add(closeMenuItem);

            menu.add(fileMenu);

            newMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        whiteboard.broadcastNewFile();
                        JOptionPane.showMessageDialog(null, "New file created");
                    } catch (RemoteException ex) {
                        System.err.println("RemoteException Is Raised When Open A New File");
                        System.exit(1);
                    }
         
                }
            });
    
    
            openMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    
                    openFile();
                    drawingPanel.repaint();
                }
            });
    
            saveMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    saveFile();
                }
            });
    
            saveAsMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    saveFileAs();
                }
            });
    
            closeMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                }
            });
            
            JMenuItem waitingRoomMenuItem = new JMenuItem("Waiting Room");
            manageMenu.add(waitingRoomMenuItem);
            
            waitingRoomMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFrame waitingUserFrame = new JFrame();
                    waitingUserFrame.setBounds(200, 150, 200, 200);
                    waitingUserFrame.setTitle("Waiting Pool");
                    
                    try {
                        ArrayList<String> waitingUserNames = whiteboard.getWaitingUserNames();

                        JPanel managePanel = new JPanel();
                        managePanel.setLayout(new GridLayout(1, 3));
                        managePanel.setPreferredSize(new Dimension(150, 150));

                        JPanel nameLebelPanel = new JPanel();
                        nameLebelPanel.setLayout(new GridLayout(waitingUserNames.size(),1));
                        nameLebelPanel.setPreferredSize(new Dimension(50, 50));

                        JPanel buttonPanel = new JPanel();
                        buttonPanel.setLayout(new GridLayout(waitingUserNames.size(),2));
                        buttonPanel.setPreferredSize(new Dimension(50, 50));




                    
                    
                        for (String name: waitingUserNames) {
                            JLabel nameLabel = new JLabel(name);

                            nameLabel.setFont(new Font("Verdana", Font.BOLD, 20));
                            nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
                            nameLabel.setVerticalAlignment(SwingConstants.CENTER);
                            nameLebelPanel.add(nameLabel);


                            if (isManager) {
                                JButton acceptButton = createManageButton(2, name);
                                JButton declineButton = createManageButton(3, name);

                                buttonPanel.add(acceptButton);
                                buttonPanel.add(declineButton);
                            }
                            
                        }

                        managePanel.add(nameLebelPanel);
                        managePanel.add(buttonPanel);
        
                        waitingUserFrame.add(managePanel, BorderLayout.CENTER);
                        waitingUserFrame.setVisible(true);

                    } catch (RemoteException e1) {
                        System.err.println("Remote Exception Is Raised When Getting User List In Waiting Room");
                        System.exit(1);

                    }
                }
            });


        } 


        menu.add(manageMenu);
        return menu;
    }

    ////////////////////////////
    ////        File        ////
    ////////////////////////////
    
    @Override
    public void resetWhiteboardState() throws RemoteException {
        restoreFromErase(blackButton);
		restoreFromText(drawingPanel);

		lastX = 0;
		lastY = 0;
		currColor = Color.BLACK;
		currFunction = "Free Draw";
		freeDrawButton.setSelected(true);
		eraserSize = "Small";

        unrepaintCount = 0;
		inputTextArea.setText("");
		colorLabel.setText("Current Color: " + "BLACK");
		eraseBox.setSelectedIndex(0);

        chatField.setText("");

        filePath = "";

        drawingPanel.repaint();
    }


    private void saveFileAs() {

		JOptionPane.showMessageDialog(null, "Please Save as A Json File");

		String preFilePath = filePath;
        
		JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(fileChooser);
        if (result == JFileChooser.APPROVE_OPTION) {
            
			
			filePath = fileChooser.getSelectedFile().getAbsolutePath();

			if (filePath.length() < 5 || !filePath.endsWith(".json")) {
				JOptionPane.showMessageDialog(null, "Please Store as A Json Filel e.g., out.json");
                filePath = preFilePath;
				return;
			}

			saveDataToFile(filePath, preFilePath);
		
            
        }
    }

    private void saveFile() {
        // If the file hasnt been save before, ask user to save the file at the expected location with expected name
		if (filePath.isEmpty()) {
			saveFileAs();
		} else {
			// Upate the canvas data at the file
			saveDataToFile(filePath, "");
		}
		
    }

    private void saveDataToFile(String filePath, String preFilePath) {
		try {
			FileWriter file = new FileWriter(filePath);

			JSONObject whiteboardJson = new JSONObject();

			JSONArray drawingListJson = new JSONArray();
            
            for (Triplet<Shape, Color, String> drawingTuple : drawingTuples) {
                JSONObject currDrawing = new JSONObject();

                currDrawing.put("Shape", getShapeJson(drawingTuple.getValue0()));
                currDrawing.put("Color", colorMap.get(drawingTuple.getValue1()));
                currDrawing.put("FunctionData", drawingTuple.getValue2());

                drawingListJson.add(currDrawing);
            }

            chatHistory = whiteboard.getChatHistory();
			JSONArray messageListJson = new JSONArray();
            for (String message : chatHistory) {
				messageListJson.add(message);
			}

            whiteboardJson.put("Drawings", drawingListJson);
			whiteboardJson.put("Messages", messageListJson);
			
			file.write(whiteboardJson.toJSONString());

			file.close();

			if (preFilePath.isEmpty()) {
				JOptionPane.showMessageDialog(null, "File Saved");
			}

		} catch (IOException e) {
			if (!preFilePath.isEmpty()) {
				filePath = preFilePath;
			} 
			JOptionPane.showMessageDialog(null, "Meet IO Exception When Storing File, Please Apply 'Save as' to Choose Another File");
		}
	}

    private JSONObject getShapeJson(Shape currShape) {
		JSONObject shapeJson = new JSONObject();

		if (currShape instanceof Line2D) {
			Line2D line = (Line2D) currShape;
			shapeJson.put("x1",  line.getX1());
			shapeJson.put("y1", line.getY1());
			shapeJson.put("x2", line.getX2());
			shapeJson.put("y2", line.getY2());
			shapeJson.put("type", 1);
		} else if (currShape instanceof Ellipse2D) {
			
			
			Ellipse2D currEllipse = (Ellipse2D) currShape;
			shapeJson.put("x", currEllipse.getX());
			shapeJson.put("y", currEllipse.getY());
			shapeJson.put("w", currEllipse.getWidth());
			shapeJson.put("h", currEllipse.getHeight());
			shapeJson.put("type", 2);
			
		} else if (currShape instanceof Rectangle2D) {
			
			Rectangle2D currRectangle = (Rectangle2D) currShape;
			shapeJson.put("x", currRectangle.getX());
			shapeJson.put("y", currRectangle.getY());
			shapeJson.put("w", currRectangle.getWidth());
			shapeJson.put("h", currRectangle.getHeight());
			shapeJson.put("type", 3);

		}
				
				
		return shapeJson;

	}


    private void openFile() {
        // Implement logic to open a file
        JFileChooser fileChooser = new JFileChooser();
		String preFilePath = filePath;

        int result = fileChooser.showOpenDialog(fileChooser);
        if (result == JFileChooser.APPROVE_OPTION) {

			try {
				String tempFilePath = fileChooser.getSelectedFile().getAbsolutePath();
				Object fileObject = new JSONParser().parse(new FileReader(tempFilePath));

                whiteboard.broadcastNewFile();
				
				loadJsonData(fileObject);

				JOptionPane.showMessageDialog(null, "File:'" + tempFilePath +  "'' Has Been Opened");
				filePath = tempFilePath;
				
			} catch (FileNotFoundException e) {
				filePath = preFilePath;
				JOptionPane.showMessageDialog(null, "File Not Found Or Have No Permission to Access");
			} catch (IOException e) {
				filePath = preFilePath;
				JOptionPane.showMessageDialog(null, "Meet IO Exception When Opening File, Please Try Another File");
			} catch (ParseException e) {
				filePath = preFilePath;
				JOptionPane.showMessageDialog(null, "Parse Exception is Raised, Please Only Open Json File Generated By Whiteboard");
			}

			

		}
    }


    private void loadJsonData(Object fileObject) {
		JSONObject whiteboardJson = (JSONObject) fileObject;

		JSONArray drawingListJson=  (JSONArray) whiteboardJson.get("Drawings");

        ArrayList<Triplet<Shape, Color, String>> tempDrawingTuples = new ArrayList<>();
        
        for (Object drawingObject : drawingListJson) {
            JSONObject currObject = (JSONObject) drawingObject;
            JSONObject currShapeObject = (JSONObject) currObject.get("Shape");

            Shape currShape;

            if ((Long) currShapeObject.get("type") == 1) {
				currShape = new Line2D.Double((double) currShapeObject.get("x1"), (double) currShapeObject.get("y1"), (double) currShapeObject.get("x2"), (double) currShapeObject.get("y2"));
			} else if ((Long) currShapeObject.get("type") == 2) {
				currShape =  new Ellipse2D.Double((double) currShapeObject.get("x"), (double) currShapeObject.get("y"), (double) currShapeObject.get("w"), (double) currShapeObject.get("h"));
			} else {
				currShape = new Rectangle2D.Double((double) currShapeObject.get("x"), (double) currShapeObject.get("y"), (double) currShapeObject.get("w"), (double) currShapeObject.get("h"));
			}

            Color currColor = colorRevMap.get(currObject.get("Color"));
			String currFunctionData = (String) currObject.get("FunctionData");

            tempDrawingTuples.add(Triplet.with(currShape, currColor, currFunctionData));
            
        }

        
        JSONArray messageListJson = (JSONArray) whiteboardJson.get("Messages");
        chatHistory = new ArrayList<>();
		for (Object message : messageListJson) {
            chatHistory.add((String) message);
        }
       
        try {
            whiteboard.broadcastDrawings(tempDrawingTuples);
            whiteboard.broadcastMessages(chatHistory);
        } catch (RemoteException e) {
            System.err.println("Remote Exception Is Raised When Oepn A File");
            System.exit(1);
        }



	}
    

    ////////////////////////////
    ////       Manage       ////
    ////////////////////////////
   

    private JButton createManageButton(int mode, String userName) {
        JButton button = new JButton();

		if (mode == 1) {
			button.setText("Kick");
		} else if (mode == 2) {
			button.setText("Accept");
		} else if (mode == 3) {
			button.setText("Decline");
		}

        button.setPreferredSize(new Dimension(20, 20));
		button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               
                try {
                    if (mode == 1) {
                        whiteboard.notifyOneClientToTerminate(userName);
                        JOptionPane.showMessageDialog(null, "Kicking user: " + userName);
                    } else if (mode == 2) {
                        
                        try {
                            if (whiteboard.acceptRequest(userName)) {
                                JOptionPane.showMessageDialog(null, "Accept user: " + userName + " Join the Whiteboard");
                            } else {
                                JOptionPane.showMessageDialog(null, "User: " + userName + " Is Not In Waiting Room");
                            }
                        } catch (Exception ex1) {
                            if (ex1 instanceof NullPointerException) {
                                System.out.println("User Has Been Removed From Waiting Room");
                            } else {
                                System.err.println(ex1.getMessage());
                            }
                        }

                       
                    } else {
                        try {
                            if (whiteboard.removeFromWaitingPool(userName)) {
                                JOptionPane.showMessageDialog(null, "Decline user: " + userName + " Join the Whiteboard");
                            } else {
                                JOptionPane.showMessageDialog(null, "User: " + userName + " Is Not In Waiting Room");
                            }
                        } catch (Exception ex1) {
                            if (ex1 instanceof NullPointerException) {
                                System.out.println("User Has Been Removed From Waiting Room");
                            } else {
                                System.err.println(ex1.getMessage());
                            }
                        }
 
                        
                    }
                    
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(null, "User: " + userName + " Is No Longer In Whiteboard/Waiting Room");
                }
    
            }
        });
		return button;
	}




    /////////////////////////////////////// 
    ////////         Main         ////////
    ///////////////////////////////////////


    private static String getUserName(IRemoteWhiteboard whiteboard) {
        String userName = JOptionPane.showInputDialog("Enter your name:");
        if (userName == null) {
            return null; 
        }
        
        try {
            
            String notification;
            while (userName.trim().isEmpty() || whiteboard.isUserNameExist(userName)) {
                notification = "Enter your name:";
                if (whiteboard.isUserNameExist(userName)) {
                    notification = "Enter a different name, previous name has been used:";
                }

                userName = JOptionPane.showInputDialog(notification);
                if (userName == null) {
                    return null; 
                }
            }
          
            
        } catch (HeadlessException e) {
            System.err.println("HeadlessException Is Raised When Getting User Names");
            System.exit(1);
        } catch (RemoteException e) {
            System.err.println("RemoteException Is Raised When Getting User Names");
            System.exit(1);
        }
        return userName;
    }

    
	public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Please Input <host> <port>");
            System.exit(1);
        }
  
        int port = 0;
		try {
			port = Integer.parseInt(args[1]);
		 } catch (NumberFormatException e) {
            System.out.println("Please Input valid format for port (Fully Integer)");
		 	System.exit(1);
		 }
    	
		
        try {
			
            Registry registry = LocateRegistry.getRegistry(args[0], port);
            IRemoteWhiteboard whiteboard = (IRemoteWhiteboard) registry.lookup("Whiteboard"); 


			String userName = getUserName(whiteboard);
			if (userName == null) {
				System.err.println("Invalid Name");
				System.exit(1);
			}
			
			new WhiteboardClient(userName, whiteboard);


            
        } catch (Exception e) {
        	if (e instanceof UnknownHostException) {
        		System.err.println("Invalid Host is Given");
        	} else if (e instanceof ConnectException) {
        		System.err.println("Server May Not Be Running Now");
        	} else if (e instanceof NotBoundException) {
        		System.err.println("Registry Server Is Not Found");
        	} else {
        		System.err.println(e.getClass().getSimpleName() + " Is Raised");
        	}
         
            System.exit(1);
        } 
    }

    





	
	 
}