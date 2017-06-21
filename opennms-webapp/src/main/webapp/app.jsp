<%--
/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
--%>


<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page="/includes/bootstrap.jsp" flush="false">
    <jsp:param name="norequirejs" value="true" />
    <jsp:param name="nobreadcrumbs" value="true" />
    <jsp:param name="useionicons" value="true"/>

    <jsp:param name="script" value='<script type="text/javascript" src="lib/angular/angular.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/angular-resource/angular-resource.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/angular-route/angular-route.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/angular-animate/angular-animate.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/angular-bootstrap/ui-bootstrap-tpls.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/underscore/underscore.js"></script>' />

    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/app.module.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/core/list/list.module.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/scanreports/scanreport.module.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/admin.module.js"></script>' />

    <jsp:param name="link" value='<link rel="stylesheet" type="text/css" href="lib/angular-loading-bar/build/loading-bar.css" />' />
    <jsp:param name="link" value='<link rel="stylesheet" type="text/css" href="lib/angular-growl-v2/build/angular-growl.css" />' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/angular-cookies/angular-cookies.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/angular-sanitize/angular-sanitize.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/angular-loading-bar/build/loading-bar.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/angular-growl-v2/build/angular-growl.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/ip-address/dist/ip-address-globals.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="lib/bootbox/bootbox.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/admin.requisition.module.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/model/RequisitionInterface.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/model/RequisitionNode.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/model/Requisition.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/model/RequisitionsData.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/model/QuickNode.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/services/Requisitions.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/services/Synchronize.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/filters/startFrom.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/directives/requisitionConstraints.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/Move.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/QuickAddNode.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/QuickAddNodeModal.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/CloneForeignSource.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/Detector.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/Policy.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/ForeignSource.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/Asset.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/Interface.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/Node.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/Requisition.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/requisition/scripts/controllers/Requisitions.js"></script>' />

    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/location/admin.location.module.js"></script>' />
    <jsp:param name="script" value='<script type="text/javascript" src="angular-app/components/admin/minion/admin.minion.module.js"></script>' />


</jsp:include>

<div ng-app="onms.ui">
    <div ng-view></div>
    <div growl></div>
</div>

<jsp:include page="/includes/bootstrap-footer.jsp" flush="false"/>