<%@include file="/WEB-INF/jsp/taglibs.jsp"%>

<div class="row">
				<div class="col-lg-12">
					<h3 class="page-header"><i class="fa fa-laptop"></i> Products</h3>
				</div>
</div>

<div class="row">
<c:choose>
	<c:when test="${products != null && products.size() >0 }">
		<table class="table table-striped table-advance table-hover">
                           <tbody>
                              <tr>
                                 <th><i class="icon_profile"></i> Product Name</th>
                                 <th><i class="icon_mobile"></i> Product Price</th>
                                 <th><i class="icon_cogs"></i> Action</th>
                              </tr>
                              <c:forEach items="${products}" var="product">
                              	<tr>
                              		<td>${product.name}</td>
                              		<td>$ ${product.price}</td>
                              		<td>
                                  		<a class="btn btn-primary" href="${pageContext.request.contextPath}/ViewProduct?productId=${product.id}" 
                                  		title="Buy" target="_blank">View</a>
                                  	</td>
                              	</tr>
                              </c:forEach>
                              </tbody>
                        </table>
		
	</c:when>
	<c:otherwise>
		<label>No Products Found...</label>
	</c:otherwise>
</c:choose>
</div>