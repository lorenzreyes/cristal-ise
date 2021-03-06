/**
 * This file is part of the CRISTAL-iSE REST API.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.restapi;

import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;

import java.util.LinkedHashMap;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.property.Property;

@Path("/item/{uuid}/property")
public class ItemProperty extends ItemUtils {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProperties(@PathParam("uuid") String uuid, @CookieParam(COOKIENAME) Cookie authCookie) {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            return toJSON(getPropertySummary(item), cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getProperty(@PathParam("uuid") String uuid, @PathParam("name") String name, @CookieParam(COOKIENAME) Cookie authCookie) {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            return item.getProperty(name);
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    @GET
    @Path("{name}/details")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPropertyDetails(@PathParam("uuid") String uuid, @PathParam("name") String name, @CookieParam(COOKIENAME) Cookie authCookie) {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        LinkedHashMap<String, Object> propDetails = new LinkedHashMap<String, Object>();

        try {
            Property prop = (Property) item.getObject(PROPERTY + "/" + name);
            propDetails.put("name", prop.getName());
            propDetails.put("value", prop.getValue());
            propDetails.put("readOnly", !prop.isMutable());

            return toJSON(propDetails, cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }
}
