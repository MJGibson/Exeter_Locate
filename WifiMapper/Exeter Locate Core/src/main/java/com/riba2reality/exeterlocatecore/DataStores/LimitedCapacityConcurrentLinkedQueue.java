package com.riba2reality.exeterlocatecore.DataStores;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LimitedCapacityConcurrentLinkedQueue {

    private final ConcurrentLinkedQueue<ServerMessage> _messageQueue =
            new ConcurrentLinkedQueue<ServerMessage>();
    private int _bytesStored = 0;
    private int _byteslimit;


    //==============================================================================================
    public LimitedCapacityConcurrentLinkedQueue(int byteslimit){

        _byteslimit = byteslimit;



    }// end of constructor
    //==============================================================================================


    //==============================================================================================
    /**
     * Adds the message parameter to the current queue, if the internal limit is exceeded then it
     * removes oldest message until it can be fit inside.
     *
     * NOTE: following this strategy if the message parameter alone is larger than the limit, all
     * other messages are removed and this is message is still added, even though it exceeds the
     * limit. Although it is expected that most messages are less than the entire limit.
     *
     * @param message
     * @return
     */
    public boolean add(ServerMessage message){
        boolean returnValue = true;

        int projectedBytesStored = _bytesStored;
        int messageNumberOfBytes =
                LimitedCapacityConcurrentLinkedQueue.getNumberOfBytes(message.message);

        if(messageNumberOfBytes > _byteslimit){
            // erm what to do if we can't fit the entire single message...
            // Since it would make later parts infintley loop, must do something........
            // sticking with current strategy, we remove all until we can add it
            _messageQueue.clear();
            _bytesStored = messageNumberOfBytes;
            return _messageQueue.add(message);
        }// end of message its self is bigger than limit

        projectedBytesStored += messageNumberOfBytes;

        if(projectedBytesStored > _byteslimit){
            returnValue = false;
            // now we need to remove the oldest, until we can fit it in

            do{

                ServerMessage removedMessage = _messageQueue.poll();
                int removedMessageNumberOfBytes =
                        LimitedCapacityConcurrentLinkedQueue.getNumberOfBytes(message.message);
                projectedBytesStored -= removedMessageNumberOfBytes;


            }while(projectedBytesStored > _byteslimit);

            returnValue = _messageQueue.add(message);

        }else{
            // within limit, so just add it and return result
            returnValue = true;

            returnValue = _messageQueue.add(message);
        }

        // update the count
        _bytesStored = projectedBytesStored;

        return returnValue;
    }// end of add
    //==============================================================================================

    //==============================================================================================
    /**
     * Returns the size in bytes of the string text. If there is encoding problem, returns the
     * number of characters.
     *
     * @param text
     * @return
     */
    private static int getNumberOfBytes(String text){
        int textNumberOfBytes;
        try {
            textNumberOfBytes = text.getBytes("UTF-8").length;
        }catch (UnsupportedEncodingException e){
            // if all else fails, assume we already did it?
            textNumberOfBytes = text.length();
        }
        return textNumberOfBytes;
    }// end of getNumberOfBytes
    //==============================================================================================


    //==============================================================================================
    /**
     * Polls the internal queue, in that it retrieves and removes the head of this queue, or returns
     * null if this queue is empty
     * @return The head of this queue, or null if the queue is empty
     */
    public ServerMessage poll(){

        return _messageQueue.poll();

    }//e dn of poll
    //==============================================================================================

    //==============================================================================================
    /**
     * Returns the size of the queue
     * @return size of the queue
     */
    public int size(){
        return _messageQueue.size();
    }
    //==============================================================================================













}//end of LimitedCapacityConcurrentLinkedQueue
//##################################################################################################
