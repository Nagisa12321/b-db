package servlet;

import utils.DBUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "BTreeServlet", value = "/BTreeServlet")
public class BTreeServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String key = request.getParameter("key");
		String res = DBUtils.get(key);
		PrintWriter writer = response.getWriter();
		writer.println(res);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String type = request.getParameter("type");
		if ("put".equals(type)) {
			String key = request.getParameter("key");
			String value = request.getParameter("value");
			DBUtils.put(key, value);
		} else if ("delete".equals(type)) {
			String key = request.getParameter("key");
			DBUtils.delete(key);
		}
	}
}
