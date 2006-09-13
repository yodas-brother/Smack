/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 * Ensure that the server is delivering messages to the correct client based on the client's
 * presence priority.
 *
 * @author Gaston Dombiak
 */
public class PresenceTest extends SmackTestCase {

    public PresenceTest(String arg0) {
        super(arg0);
    }

    /**
     * Connection(0) will send messages to the bareJID of Connection(1) where the user of
     * Connection(1) has logged from two different places with different presence priorities.
     */
    public void testMessageToHighestPriority() {
        XMPPConnection conn = null;
        try {
            // User_1 will log in again using another resource
            conn = new XMPPConnection(getHost(), getPort());
            conn.login(getUsername(1), getUsername(1), "OtherPlace");
            // Change the presence priorities of User_1
            getConnection(1).sendPacket(new Presence(Presence.Type.available, null, 1,
                    Presence.Mode.available));
            conn.sendPacket(new Presence(Presence.Type.available, null, 2,
                    Presence.Mode.available));
            Thread.sleep(150);
            // Create the chats between the participants
            Chat chat0 = new Chat(getConnection(0), getBareJID(1));
            Chat chat1 = new Chat(getConnection(1), getBareJID(0), chat0.getThreadID());
            Chat chat2 = new Chat(conn, getBareJID(0), chat0.getThreadID());

            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            assertNotNull("Resource with highest priority didn't receive the message",
                    chat2.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat1.nextMessage(1000));

            // Invert the presence priorities of User_1
            getConnection(1).sendPacket(new Presence(Presence.Type.available, null, 2,
                    Presence.Mode.available));
            conn.sendPacket(new Presence(Presence.Type.available, null, 1,
                    Presence.Mode.available));

            Thread.sleep(150);
            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            assertNotNull("Resource with highest priority didn't receive the message",
                    chat1.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat2.nextMessage(1000));

            // User_1 closes his connection
            chat2 = null;
            conn.close();
            Thread.sleep(150);

            // Test delivery of message to the unique presence of the user_1
            chat0.sendMessage("Hello");
            assertNotNull("Resource with highest priority didn't receive the message",
                    chat1.nextMessage(2000));

            getConnection(1).sendPacket(new Presence(Presence.Type.available, null, 2,
                    Presence.Mode.available));

            // User_1 will log in again using another resource
            conn = new XMPPConnection(getHost(), getPort());
            conn.login(getUsername(1), getUsername(1), "OtherPlace");
            conn.sendPacket(new Presence(Presence.Type.available, null, 1,
                    Presence.Mode.available));
            chat2 = new Chat(conn, getBareJID(0), chat0.getThreadID());

            Thread.sleep(150);
            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            assertNotNull("Resource with highest priority didn't receive the message",
                    chat1.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat2.nextMessage(1000));

            // Invert the presence priorities of User_1
            getConnection(1).sendPacket(new Presence(Presence.Type.available, null, 1,
                    Presence.Mode.available));
            conn.sendPacket(new Presence(Presence.Type.available, null, 2,
                    Presence.Mode.available));

            Thread.sleep(150);
            // Test delivery of message to the presence with highest priority
            chat0.sendMessage("Hello");
            assertNotNull("Resource with highest priority didn't receive the message",
                    chat2.nextMessage(2000));
            assertNull("Resource with lowest priority received the message",
                    chat1.nextMessage(1000));

        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    /**
     * User1 logs from 2 resources but only one is available. User0 sends a message
     * to the full JID of the unavailable resource. User1 in the not available resource
     * should receive the message.
     */
    public void testNotAvailablePresence() throws XMPPException {
        // Change the presence to unavailable of User_1
        getConnection(1).sendPacket(new Presence(Presence.Type.unavailable));

        // User_1 will log in again using another resource (that is going to be available)
        XMPPConnection conn = new XMPPConnection(getHost(), getPort());
        conn.login(getUsername(1), getUsername(1), "OtherPlace");

        // Create chats between participants
        Chat chat0 = new Chat(getConnection(0), getFullJID(1));
        Chat chat1 = new Chat(getConnection(1), getBareJID(0), chat0.getThreadID());

        // Test delivery of message to the presence with highest priority
        chat0.sendMessage("Hello");
        assertNotNull("Not available connection didn't receive message sent to full JID",
                chat1.nextMessage(2000));
        assertNull("Not available connection received an unknown message",
                chat1.nextMessage(1000));

    }

    protected int getMaxConnections() {
        return 2;
    }
}