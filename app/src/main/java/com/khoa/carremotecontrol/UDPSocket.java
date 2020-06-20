package com.khoa.carremotecontrol;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSocket {

    private DatagramPacket datagramPacket;
    private DatagramSocket datagramSocket;
    private InetAddress inetAddress;
    private byte[] bufferByte = new byte[16];
    private String address = "";
    private int port;

    public UDPSocket(String address, int port) {
        this.address = address;
        this.port = port;
        try {
            datagramSocket = new DatagramSocket();
            inetAddress = InetAddress.getByName(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendSignal(String string) {
        try {
            datagramPacket = new DatagramPacket(string.getBytes(), string.length(), inetAddress, 6868);
            datagramSocket.send(datagramPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receiveSignal() {
        try {
            datagramPacket = new DatagramPacket(bufferByte, bufferByte.length, inetAddress, port);
            datagramSocket.receive(datagramPacket);
            byte[] data = datagramPacket.getData();
            int length = 0;
            for (int i = 0; i < data.length; i++) {
                if ((int) data[i] != 0) length++;
            }
            String string = new String(data, 0, length);
            Log.e("Loi", "receive: " + string + " , length: " + string.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
