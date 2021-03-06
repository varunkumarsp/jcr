/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.webdav.command.order;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.rest.ext.provider.HierarchicalPropertyEntityProvider;
import org.exoplatform.services.rest.impl.ContainerResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;

import javax.jcr.Node;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS. Author : Vitaly Guly <gavrikvetal@gmail.com>
 * 
 * @version $Id: $
 */

public class TestOrderBefore extends OrderPatchTest
{

   protected Node orderBeforeNode;

   public void setUp() throws Exception
   {
      super.setUp();
      session.refresh(false);
      if (orderBeforeNode == null)
      {
         orderBeforeNode = orderPatchNode.addNode("orderBeforeNode", ORDERABLE_NODETYPE);
         session.save();
         for (int i = 1; i <= 5; i++)
         {
            orderBeforeNode.addNode("n" + i, ORDERABLE_NODETYPE);
         }
         session.save();
      }
   }

   public void testOrderBefore1() throws Exception
   {
      assertOrder(orderBeforeNode, new String[]{"n1", "n2", "n3", "n4", "n5"});

      String path = orderBeforeNode.getPath();

      String xml =
         "" + "<D:orderpatch xmlns:D=\"DAV:\">" + "<D:order-member>" + "<D:segment>n3</D:segment>" + "<D:position>"
            + "<D:before>" + "<D:segment>n2</D:segment>" + "</D:before>" + "</D:position>" + "</D:order-member>"
            + "</D:orderpatch>";

      ContainerResponse response =
         service(WebDAVMethods.ORDERPATCH, getPathWS() + URLEncoder.encode(path, "UTF-8"), "", null, xml.getBytes());
      assertEquals(HTTPStatus.OK, response.getStatus());

      assertOrder(orderBeforeNode, new String[]{"n1", "n3", "n2", "n4", "n5"});
   }

   public void testOrderBefore2() throws Exception
   {
      assertOrder(orderBeforeNode, new String[]{"n1", "n2", "n3", "n4", "n5"});

      String path = orderBeforeNode.getPath();

      String xml =
         "" + "<D:orderpatch xmlns:D=\"DAV:\">" + "<D:order-member>" + "<D:segment>n4</D:segment>" + "<D:position>"
            + "<D:before>" + "<D:segment>n2</D:segment>" + "</D:before>" + "</D:position>" + "</D:order-member>" +

            "<D:order-member>" + "<D:segment>n1</D:segment>" + "<D:position>" + "<D:before>"
            + "<D:segment>n0</D:segment>" + "</D:before>" + "</D:position>" + "</D:order-member>" +

            "</D:orderpatch>";

      // HierarchicalProperty body = body(xml);

      ContainerResponse response =
         service(WebDAVMethods.ORDERPATCH, getPathWS() + URLEncoder.encode(path, "UTF-8"), "", null, xml.getBytes());
      assertEquals(HTTPStatus.MULTISTATUS, response.getStatus());
      OrderPatchResponseEntity entity = (OrderPatchResponseEntity)response.getEntity();
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      entity.write(outStream);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      entity.write(outputStream);
      String resp = outputStream.toString();
      HierarchicalPropertyEntityProvider entityProvider = new HierarchicalPropertyEntityProvider();
      HierarchicalProperty multistatus =
         entityProvider.readFrom(null, null, null, null, null, new ByteArrayInputStream(resp.getBytes()));
      assertEquals(new QName("DAV:", "multistatus"), multistatus.getName());
      assertEquals(2, multistatus.getChildren().size());

      HierarchicalProperty response1 = multistatus.getChild(0);

      String href1MustBe = TextUtil.escape(getPathWS() + orderBeforeNode.getPath() + "/n4", '%', true);
      String href1 = response1.getChild(new QName("DAV:", "href")).getValue();

      assertEquals(href1MustBe, href1);

      String status1 = WebDavConst.getStatusDescription(HTTPStatus.OK);
      assertEquals(status1, response1.getChild(new QName("DAV:", "status")).getValue());

      HierarchicalProperty response2 = multistatus.getChild(1);

      String href2MustBe = TextUtil.escape(getPathWS() + orderBeforeNode.getPath() + "/n1", '%', true);
      String href2 = response2.getChild(new QName("DAV:", "href")).getValue();

      assertEquals(href2MustBe, href2);

      String status2 = WebDavConst.getStatusDescription(HTTPStatus.FORBIDDEN);
      assertEquals(status2, response2.getChild(new QName("DAV:", "status")).getValue());

      assertOrder(orderBeforeNode, new String[]{"n1", "n4", "n2", "n3", "n5"});
   }

}
