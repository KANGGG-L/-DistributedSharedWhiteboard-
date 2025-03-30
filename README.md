# Distributed Shared Whiteboard

A real-time collaborative whiteboard application built with Java RMI, enabling multiple users to draw, chat, and manage sessions synchronously.
![Image](https://github.com/user-attachments/assets/57ced7c2-8af7-4897-8894-884c6d161b91)

## Features
- **Real-Time Drawing**:  
  - Freehand drawing, shapes (lines, circles, rectangles), text input, and 16 colors.  
  - Synchronized canvas updates across all clients using the **Observer pattern**.  
- **User Management**:  
  - **Manager-peer model**: First user becomes manager with privileges to approve/kick users.  
  - Waiting pool for join requests and active user list.  
- **Chat System**:  
  - Integrated text chat for user communication.  
- **File Operations**:  
  - Save/load whiteboard state as JSON (manager-only).  
- **Fault Tolerance**:  
  - Handles network outages and client disconnections gracefully.  

## Architecture
- **Client-Server Model**:  
  - **Server**: Manages connections (RMI), broadcasts updates, and enforces synchronization.  
  - **Client**: Java Swing GUI for user interaction, registers as an observer for real-time updates.  
- **Design Patterns**:  
  - Observer pattern for state synchronization.  
  - Modular GUI components for scalability.
 
## Class Diagram
![Image](https://github.com/user-attachments/assets/56743c96-49e5-4975-9b88-e83d162d844b)

## Interactive Diagram
![Image](https://github.com/user-attachments/assets/5657278b-1381-4fab-ae26-91c719e53fef)
