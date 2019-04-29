package server.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("houses")
public class HousesService {

	@Path("add")
	@POST
	@Consumes({ "application/json" })
	@Produces({ "application/json" })
	public Response getUsersList() {

		return null;
	}

}
