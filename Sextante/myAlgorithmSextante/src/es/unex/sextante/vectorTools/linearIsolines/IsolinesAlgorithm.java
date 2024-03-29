/****************************************************************************
 *	Sextante - Geospatial analysis tools
 *  www.sextantegis.com
 *  (C) 2009
 *    
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 *	along with this program; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *    
 *    @author      	Josef Bezdek, ZCU Plzen
 *	  @version     	1.0
 *    @since 		JDK1.5 
 */

package es.unex.sextante.vectorTools.linearIsolines;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import org.geotools.index.Data;
import org.geotools.index.DataDefinition;
import org.geotools.index.rtree.PageStore;
import org.geotools.index.rtree.RTree;
import org.geotools.index.rtree.memory.MemoryPageStore;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.outputs.OutputVectorLayer;
import es.unex.sextante.vectorTools.bezierSurface.Bezier;
import es.unex.sextante.vectorTools.bezierSurface.BezierSurface;

public class IsolinesAlgorithm extends GeoAlgorithm {
	
	public static final String TIN = "TIN";
	public static final String ISOLINES = "ISOLINES";
	public static final String EQUIDISTANCE = "EQUIDISTANCE";
	public static final String LoD = "LoD";
	public static final String ClusterTol = "ClusterTol";
	public static final String Smooth = "Smooth";
	
	private IVectorLayer m_Triangles;
	private IVectorLayer m_Isolines;
	private double m_EquiDistance;
	private int m_LoD;
	private double m_ClusterTol;
	private double m_Smooth;
	
	private Data data;
	private DataDefinition dd = new DataDefinition("US-ASCII"); 
	
	private RTree trianglesIndex;
	Coordinate [][] triangles;
	TreeMap breakLines = new TreeMap();
	Bezier miniBezierTriangles[];
	double scaleZ;
	
	public void defineCharacteristics() {

		setName(Sextante.getText( "TIN - extracting isolines"));
		setGroup(Sextante.getText("Herramientas_capas_puntos"));
		setGeneratesUserDefinedRasterOutput(false);
		String[] sDistance = {"0","1","2","3","4","5","6","7","8","9"};

		try {
			m_Parameters.addInputVectorLayer(TIN,
											Sextante.getText( "TIN"),
											AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON,
											true);
		
			m_Parameters.addNumericalValue(EQUIDISTANCE,
					Sextante.getText( "   Equidistance"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE,
					10,
					0,
					Double.MAX_VALUE);
			
			m_Parameters.addSelection(LoD,
					Sextante.getText( "   Level of Detail"),
					sDistance);

			m_Parameters.addNumericalValue(ClusterTol,
					Sextante.getText( "   Cluster tolerance"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE,
					0.001,
					0,
					Double.MAX_VALUE);
			m_Parameters.addNumericalValue(Smooth,
					Sextante.getText( "   Smoothing coeficient (0.1 - 1)"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE,
					1,
					0.1,
					1);

			
			addOutputVectorLayer(ISOLINES,
											Sextante.getText( "Resultado"),
											OutputVectorLayer.SHAPE_TYPE_POLYGON);
		} catch (Exception e) {
			Sextante.addErrorToLog(e);
		}

	}

	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

		int i;
		int iShapeCount;
		double maxZValue = Double.NEGATIVE_INFINITY;
		double minZValue = Double.POSITIVE_INFINITY;
		
		m_Triangles = m_Parameters.getParameterValueAsVectorLayer(TIN);
		m_EquiDistance = m_Parameters.getParameterValueAsDouble(EQUIDISTANCE);
		m_LoD = m_Parameters.getParameterValueAsInt(LoD);
		m_ClusterTol = m_Parameters.getParameterValueAsDouble(ClusterTol);
		m_Smooth = m_Parameters.getParameterValueAsDouble(Smooth);
		
		Class types[] = {Integer.class, Double.class};
		String sNames[] = {"ID","Value"};
		m_Isolines = getNewVectorLayer(ISOLINES,
										m_Triangles.getName()+"_Isolines",
										IVectorLayer.SHAPE_TYPE_POLYGON,
										types,
										sNames);

		i = 0;
		iShapeCount = m_Triangles.getShapesCount();
		triangles = new Coordinate[iShapeCount][3];
		IFeatureIterator iter = m_Triangles.iterator();
		try{ 
			dd.addField(Integer.class);
			PageStore ps = new MemoryPageStore(dd);
			trianglesIndex = new RTree(ps);
			while(iter.hasNext()){
				IFeature feature = iter.next();
				Polygon trianglePolygon = (Polygon) feature.getGeometry();
				IRecord record = feature.getRecord();
				if (((String)record.getValue(1)) == "Y")
					breakLines.put(i, (Integer)record.getValue(2));
				
				triangles [i][0] = (Coordinate) trianglePolygon.getCoordinates()[0].clone();
				triangles [i][1] = (Coordinate) trianglePolygon.getCoordinates()[1].clone();
				triangles [i][2] = (Coordinate) trianglePolygon.getCoordinates()[2].clone();

				data = new Data(dd);
				data.addValue(i);
				trianglesIndex.insert(trianglePolygon.getEnvelopeInternal(), data);
	
				for (int k=0; k<2; k++){
					double diffZ = triangles[i][k].z - triangles[i][k+1].z;
					double diffXY = Math.sqrt(Math.pow((triangles[i][k].x-triangles[i][k+1].x),2)+
								Math.pow((triangles[i][k].y-triangles[i][k+1].y),2));
				
					if (scaleZ < Math.abs(diffZ/diffXY)){
						scaleZ = Math.abs(diffZ/diffXY);
					}
				}	
				setProgress(i,2*iShapeCount);
				i++;
			}
			iter.close();
		}
		catch (Exception e){
			e.printStackTrace(); 
		}
		m_Triangles = null;
		iter = null;

		LinearContourLines isoLineFactory = new LinearContourLines(m_EquiDistance, m_ClusterTol);
			
			
		if (m_LoD != 0){
			BezierSurface bezierSurface = new BezierSurface(triangles, trianglesIndex, breakLines, scaleZ*m_Smooth, m_LoD);
			while (bezierSurface.hasNext()){
				setProgress(i++,2*iShapeCount);
				Coordinate newTin[][] = bezierSurface.nextTrinagle();
				isoLineFactory.countIsolines(newTin);
			}	
		}
		else
			isoLineFactory.countIsolines(triangles);
				
		ArrayList isolines = isoLineFactory.getIsolines();
		Iterator iterIso = isolines.iterator();
		int j=0;
		for (int l=0; l < isolines.size(); l++){
			Object o = isolines.get(l);
			if (o != null){
				GeometryFactory gf = new GeometryFactory();
				Iterator isoL = ((LinkedList)o).iterator();
				Coordinate[] coords = new Coordinate[((LinkedList)o).size()];
				int k = 0;
				while(isoL.hasNext()){
					coords[k] = (Coordinate)isoL.next();
					k++;
				}	
				Object[] record = {new Integer(j),coords[0].z};
				LineString isoline = gf.createLineString(coords);
				m_Isolines.addFeature(isoline, record);
				j++;
						
			}
		}
			
		return !m_Task.isCanceled();
	}	
}

