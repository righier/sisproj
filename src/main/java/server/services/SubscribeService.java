package server.services;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import server.Admins;

@Path("admin")
public class SubscribeService {

	@GET
	@Path("watch/{ip}/{port}")
	public Response watch(@PathParam("ip") String ip, @PathParam("port") int port) {
		if (Admins.add(ip+port)) {
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.CONFLICT).build();
		}
	}

	@GET
	@Path("stop/{ip}/{port}")
	public Response stop(@PathParam("ip") String ip, @PathParam("port") int port) {
		if (Admins.remove(ip+port)) {
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

}
