package net.mzimmer.android.apps.rotation;

import android.hardware.SensorEvent;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class SensorEventPacketFactory {
	public static final int EXPONENT = -9;
	public static final int FACTOR;

	static {
		FACTOR = (int) Math.pow(10, -EXPONENT);
	}

	private final SocketAddress socketAddress;

	public SensorEventPacketFactory(SocketAddress socketAddress) {
		this.socketAddress = socketAddress;
	}

	public DatagramPacket from(SensorEvent event) throws SocketException {
		ByteBuffer byteBuffer = ByteBuffer.allocate(8 + 4 * 4).order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putLong(event.timestamp);
		for (int i = 0; i < 4; ++i) {
			byteBuffer.putFloat(event.values[i]);
		}
		byte[] data = byteBuffer.array();
		return new DatagramPacket(data, data.length, socketAddress);
	}
}
