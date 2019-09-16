package server.services;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import beans.House;
import server.Admins.AdminChannel;
import utils.Helper;
import utils.StreamHelper;
import server.Admins;
import server.Houses;

@Path("houses")
public class HousesService {
	static final String json = "application/json";

	@GET
	@Produces({json})
	public Response getList() {
		return Response.ok(Houses.getList()).build();
	}

	@POST
	@Path("add")
	@Consumes({json})
	@Produces({json})
	public Response addHouse(House house) {
		List<House> list = Houses.addAndGetList(house);
		if (list != null) {
			for (AdminChannel ch: Admins.getChannels()) {
				ch.async().notifyHouseJoined(Helper.id(house.getId()), StreamHelper.ignore());
			}
			return Response.ok(list).build();
		} else {
			return Response.status(Response.Status.CONFLICT).build();
		}
	}

	@DELETE
	@Path("remove/{id}")
	public Response removeHouse(@PathParam("id") String id) {
		if (Houses.remove(id)) {
			for (AdminChannel ch: Admins.getChannels()) {
				ch.async().notifyHouseLeft(Helper.id(id), StreamHelper.ignore());
			}
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}
	
	@GET
	@Path("boost/start/{id}")
	public Response startBoost(@PathParam("id") String id) {
		if (Houses.exists(id)) {
			for (AdminChannel ch: Admins.getChannels()) {
				ch.async().notifyBoostStart(Helper.id(id), StreamHelper.ignore());
			}
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}
	
	@GET
	@Path("boost/stop/{id}")
	public Response stopBoost(@PathParam("id") String id) {
		if (Houses.exists(id)) {
			for (AdminChannel ch: Admins.getChannels()) {
				ch.async().notifyBoostStop(Helper.id(id), StreamHelper.ignore());
			}
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}
}
