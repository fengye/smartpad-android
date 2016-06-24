package net.mzimmer.android.apps.smartpad;

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
		ByteBuffer byteBuffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
		try {
			UnityQuaternion q = UnityQuaternion.from(event);
			byteBuffer.putFloat(q.x);
			byteBuffer.putFloat(q.y);
			byteBuffer.putFloat(q.z);
			byteBuffer.putFloat(q.w);
		} catch (IllegalArgumentException e) {
			byteBuffer.putFloat(0.0f);
			byteBuffer.putFloat(0.0f);
			byteBuffer.putFloat(0.0f);
			byteBuffer.putFloat(1.0f);
		}
		byte[] data = byteBuffer.array();
		return new DatagramPacket(data, data.length, socketAddress);
	}
}
