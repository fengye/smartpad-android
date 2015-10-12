package net.mzimmer.android.apps.rotation;

import android.hardware.SensorEvent;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class SensorEventPacketFactory {
	private final SocketAddress socketAddress;

	public SensorEventPacketFactory(SocketAddress socketAddress) {
		this.socketAddress = socketAddress;
	}

	public DatagramPacket from(SensorEvent event) throws SocketException {
		ByteBuffer byteBuffer = ByteBuffer.allocate(8 + 4 + 4 * event.values.length).order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putLong(event.timestamp);
		byteBuffer.putInt(event.values.length);
		for (int i = 0; i < event.values.length; ++i) {
			byteBuffer.putFloat(event.values[i]);
		}
		byte[] data = byteBuffer.array();
		return new DatagramPacket(data, data.length, socketAddress);
	}
}
