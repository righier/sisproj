package utils;

import proto.HouseProto.Identifier;

public class Helper {

	public static Identifier id(String id) {
		return Identifier.newBuilder().setId(id).build();
	}
}
