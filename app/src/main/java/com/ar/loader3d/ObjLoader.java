package com.ar.loader3d;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Basic OBJ file loader
 */
public class ObjLoader {
    public class Vector2<T> {
        public T x, y;

        public Vector2() {
            x = (T) Double.valueOf(0.);
            y = (T) Double.valueOf(0.);
        }

        public Vector2(T x, T y) {
            this.x = x;
            this.y = y;
        }
    }

    public class Vector3<T> {
        public T x, y, z;
    }

    public class Face implements Comparable<Face> {
        public Face() {
            indices = new ArrayList<Integer>();
            normals = new ArrayList<Integer>();
            texcoords = new ArrayList<Integer>();
        }

        public ArrayList<Integer> indices;
        public ArrayList<Integer> normals;
        public ArrayList<Integer> texcoords;

        protected double meanVal(int coord) {
            Integer[] a = {1, 2, 3};
            Vector3<Double> p1 = vertices.get(indices.get(0) - 1);
            Vector3<Double> p2 = vertices.get(indices.get(1) - 1);
            Vector3<Double> p3 = vertices.get(indices.get(2) - 1);
            if (coord == 0)
                return p1.x + p2.x + p3.x / 3.;
            if (coord == 1)
                return p1.y + p2.y + p3.y / 3.;
            if (coord == 2)
                return p1.z + p2.z + p3.z / 3.;
            else
                try {
                    throw new Exception("wrong value");
                } catch (Exception e) {
                    e.printStackTrace();
                    return 0;
                }
            //Vector3<Double>[] pointArray = {vertices.get(indices.get(0)), vertices.get(indices.get(0)), vertices.get(indices.get(0))};
        }

        @Override
        public int compareTo(Face other) {

            int coord = 2;
            if (meanVal(coord) < other.meanVal(coord))
                return -1;
            if (meanVal(coord) > other.meanVal(coord))
                return 1;
            if (meanVal(coord) == other.meanVal(coord))
                return 0;
            return 0;
        }

    }

    public ArrayList<Vector3<Double>> vertices;
    public ArrayList<Vector3<Double>> normals;
    public ArrayList<Vector2<Double>> texcoords;
    public ArrayList<Face> faces;

    public ObjLoader(String filename, boolean swapyz) {
        //Loads a Wavefront OBJ file.
        vertices = new ArrayList<Vector3<Double>>();
        normals = new ArrayList<Vector3<Double>>();
        texcoords = new ArrayList<Vector2<Double>>();
        faces = new ArrayList<Face>();

        //material = None
        BufferedReader file = null;
        try {
            file = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = null;

        while (true) {
            try {
                line = file.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (line == null)
                break;
            if (line.startsWith("#"))
                continue;
            String[] values = line.split(" ");
            if (values.length == 0)
                continue;
            if (values[0].equals("v")) {
                Vector3<Double> v = new Vector3<Double>();
                v.x = Double.valueOf(values[1]);
                //vertices.add(Float.valueOf(values[1]));
                if (swapyz) {
                    v.y = Double.valueOf(values[3]);
                    v.z = Double.valueOf(values[2]);
                    //vertices.add(Float.valueOf(values[3]));
                    //vertices.add(Float.valueOf(values[2]));
                } else {
                    v.y = Double.valueOf(values[2]);
                    v.z = Double.valueOf(values[3]);
                    //vertices.add(Float.valueOf(values[2]));
                    //vertices.add(Float.valueOf(values[3]));
                }
                vertices.add(v);
            } else if (values[0].equals("vn")) {
                Vector3<Double> n = new Vector3<Double>();
                n.x = Double.valueOf(values[1]);
                //vertices.add(Float.valueOf(values[1]));
                if (swapyz) {
                    n.y = Double.valueOf(values[3]);
                    n.z = Double.valueOf(values[2]);
                    //vertices.add(Float.valueOf(values[3]));
                    //vertices.add(Float.valueOf(values[2]));
                } else {
                    n.y = Double.valueOf(values[2]);
                    n.z = Double.valueOf(values[3]);
                    //vertices.add(Float.valueOf(values[2]));
                    //vertices.add(Float.valueOf(values[3]));
                }
                normals.add(n);
            } else if (values[0].equals("vt")) {
                Vector2<Double> t = new Vector2<Double>();
                t.x = Double.valueOf(values[1]);
                t.y = Double.valueOf(values[2]);
                texcoords.add(t);
//	    			texcoords.add(Float.valueOf(values[1]));
//	    			texcoords.add(Float.valueOf(values[2]));
            } else if (values[0].equals("f")) {
                Face face = new Face();
                for (int i = 1; i < values.length; i++) {
                    String[] triplet = values[i].split("/");
                    face.indices.add(Integer.parseInt(triplet[0]));
                    if (triplet.length >= 2)
                        face.texcoords.add(Integer.parseInt(triplet[1]));
                    else {
                        face.texcoords.add(0);
                    }
                    if (triplet.length >= 3)
                        face.normals.add(Integer.parseInt(triplet[2]));
                    else
                        face.normals.add(0);
                }
                faces.add(face);
            } else {

            }
        }

        Collections.sort(faces);
        //self.faces = sorted(self.faces, key=self.zOrder)
    }
}
