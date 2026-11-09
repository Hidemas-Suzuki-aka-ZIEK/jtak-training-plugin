package jp.jtak.core.lrf;

/**
 * 楕円体上の測地計算（Vincenty 1975 の順解法と逆解法。出典 docs/references.md S2）。
 * <p>
 * Geodesic calculations on an ellipsoid (Vincenty 1975 direct and inverse methods; source: docs/references.md S2).
 */
public final class Geodesy {
    private static final double A = 6378137.0;
    private static final double F = 1.0 / 298.257223563;
    private static final double B = A * (1.0 - F);
    private static final int MAX_ITERATIONS = 200;
    private static final double EPSILON = 1e-12;

    private Geodesy() {}

    /**
     * 始点から方位と距離で進んだ点（緯度・経度、度）を返す。楕円体は WGS84。
     *
     * <p>使う範囲は LRF の距離（数十 km まで）。対蹠点の近く（約 20,000 km）は対象外。
     * 反復が 200 回で収束しなければ {@link IllegalStateException}。経度は (-180, 180]。
     * <p>
     * Returns the point reached from the starting point using a bearing and distance (latitude and longitude in
     * degrees). Uses the WGS84 ellipsoid. <p> Intended for LRF distances (up to tens of km). Near-antipodal
     * distances (about 20,000 km) are out of scope. Throws {@link IllegalStateException} if iteration does not
     * converge within 200 iterations. Longitude is in (-180, 180].
     *
     * @return {緯度, 経度}（度） {latitude, longitude} in degrees
     */
    public static double[] direct(double latDeg, double lonDeg, double azimuthDeg, double distanceMeters) {
        if (distanceMeters == 0.0) {
            return new double[] {latDeg, normalizeLon(lonDeg)};
        }
        double phi1 = Math.toRadians(latDeg);
        double alpha1 = Math.toRadians(azimuthDeg);
        double sinAlpha1 = Math.sin(alpha1);
        double cosAlpha1 = Math.cos(alpha1);

        double tanU1 = (1.0 - F) * Math.tan(phi1);
        double cosU1 = 1.0 / Math.sqrt(1.0 + tanU1 * tanU1);
        double sinU1 = tanU1 * cosU1;

        double sigma1 = Math.atan2(tanU1, cosAlpha1);
        double sinAlpha = cosU1 * sinAlpha1;
        double cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
        double uSq = cosSqAlpha * (A * A - B * B) / (B * B);
        double bigA = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)));
        double bigB = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)));

        double sigma = distanceMeters / (B * bigA);
        double sinSigma;
        double cosSigma;
        double cos2SigmaM;
        int iterations = 0;
        while (true) {
            cos2SigmaM = Math.cos(2.0 * sigma1 + sigma);
            sinSigma = Math.sin(sigma);
            cosSigma = Math.cos(sigma);
            double deltaSigma = bigB * sinSigma * (cos2SigmaM + bigB / 4.0
                    * (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) - bigB / 6.0 * cos2SigmaM
                            * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)));
            double sigmaNext = distanceMeters / (B * bigA) + deltaSigma;
            double change = Math.abs(sigmaNext - sigma);
            sigma = sigmaNext;
            if (change < EPSILON) {
                break;
            }
            if (++iterations >= MAX_ITERATIONS) {
                throw new IllegalStateException("Vincenty の順解法が収束しません");
            }
        }
        // 収束した sigma で最終値を再計算する。
        // Recalculate final values using the converged sigma.
        cos2SigmaM = Math.cos(2.0 * sigma1 + sigma);
        sinSigma = Math.sin(sigma);
        cosSigma = Math.cos(sigma);

        double tmp = sinU1 * sinSigma - cosU1 * cosSigma * cosAlpha1;
        double phi2 = Math.atan2(sinU1 * cosSigma + cosU1 * sinSigma * cosAlpha1,
                (1.0 - F) * Math.sqrt(sinAlpha * sinAlpha + tmp * tmp));
        double lambda = Math.atan2(sinSigma * sinAlpha1,
                cosU1 * cosSigma - sinU1 * sinSigma * cosAlpha1);
        double c = F / 16.0 * cosSqAlpha * (4.0 + F * (4.0 - 3.0 * cosSqAlpha));
        double bigL = lambda - (1.0 - c) * F * sinAlpha * (sigma + c * sinSigma
                * (cos2SigmaM + c * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)));
        double lon2 = lonDeg + Math.toDegrees(bigL);
        return new double[] {Math.toDegrees(phi2), normalizeLon(lon2)};
    }

    /**
     * 2 点の距離と、始点での前方位を返す（Vincenty 1975 の逆解法。出典 docs/references.md S2）。楕円体は WGS84。
     *
     * <p>2 点が同じなら {0, NaN}。対蹠点の近く（約 20,000 km）は対象外で、反復が 200 回で
     * 収束しなければ {@link IllegalStateException}。
     * <p>
     * Returns the distance between two points and the forward azimuth at the starting point (Vincenty 1975 inverse
     * method; source: docs/references.md S2). Uses the WGS84 ellipsoid. <p> Returns {0, NaN} for coincident
     * points. Near-antipodal distances (about 20,000 km) are out of scope; throws {@link IllegalStateException} if
     * iteration does not converge within 200 iterations.
     *
     * @return {距離（m）, 始点での前方位（度。真北から時計回り、[0, 360)）} {distance (m), forward azimuth at the starting point (degrees clockwise from true north, [0, 360))}
     */
    public static double[] inverse(double lat1Deg, double lon1Deg, double lat2Deg, double lon2Deg) {
        if (lat1Deg == lat2Deg && normalizeLon(lon1Deg) == normalizeLon(lon2Deg)) {
            return new double[] {0.0, Double.NaN};
        }
        double bigL = Math.toRadians(normalizeLon(lon2Deg - lon1Deg));
        double u1 = Math.atan((1.0 - F) * Math.tan(Math.toRadians(lat1Deg)));
        double u2 = Math.atan((1.0 - F) * Math.tan(Math.toRadians(lat2Deg)));
        double sinU1 = Math.sin(u1);
        double cosU1 = Math.cos(u1);
        double sinU2 = Math.sin(u2);
        double cosU2 = Math.cos(u2);

        double lambda = bigL;
        double sinLambda;
        double cosLambda;
        double sinSigma;
        double cosSigma;
        double sigma;
        double cosSqAlpha;
        double cos2SigmaM;
        int iterations = 0;
        while (true) {
            sinLambda = Math.sin(lambda);
            cosLambda = Math.cos(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            sinSigma = Math.sqrt(t1 * t1 + t2 * t2);
            if (sinSigma == 0.0) {
                return new double[] {0.0, Double.NaN};
            }
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            // 赤道上の線では cosSqAlpha が 0 になるので、cos2σm を 0 とする。
            // For an equatorial line, cosSqAlpha is 0, so set cos2σm to 0.
            cos2SigmaM = cosSqAlpha == 0.0 ? 0.0 : cosSigma - 2.0 * sinU1 * sinU2 / cosSqAlpha;
            double c = F / 16.0 * cosSqAlpha * (4.0 + F * (4.0 - 3.0 * cosSqAlpha));
            double lambdaNext = bigL + (1.0 - c) * F * sinAlpha * (sigma + c * sinSigma
                    * (cos2SigmaM + c * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)));
            double change = Math.abs(lambdaNext - lambda);
            lambda = lambdaNext;
            if (change < EPSILON) {
                break;
            }
            iterations++;
            if (iterations >= MAX_ITERATIONS) {
                throw new IllegalStateException("Vincenty の逆解法が収束しません");
            }
        }
        // 収束した λ で最終値を再計算する。
        // Recalculate final values using the converged λ.
        sinLambda = Math.sin(lambda);
        cosLambda = Math.cos(lambda);
        double t1 = cosU2 * sinLambda;
        double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
        sinSigma = Math.sqrt(t1 * t1 + t2 * t2);
        cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
        sigma = Math.atan2(sinSigma, cosSigma);
        double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
        cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
        cos2SigmaM = cosSqAlpha == 0.0 ? 0.0 : cosSigma - 2.0 * sinU1 * sinU2 / cosSqAlpha;

        double uSq = cosSqAlpha * (A * A - B * B) / (B * B);
        double bigA = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)));
        double bigB = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)));
        double deltaSigma = bigB * sinSigma * (cos2SigmaM + bigB / 4.0
                * (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) - bigB / 6.0 * cos2SigmaM
                        * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)));
        double distance = B * bigA * (sigma - deltaSigma);
        double alpha1 = Math.atan2(cosU2 * sinLambda, cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
        double bearing = Math.toDegrees(alpha1);
        bearing = bearing % 360.0;
        if (bearing < 0.0) {
            bearing += 360.0;
        }
        if (bearing >= 360.0) {
            bearing = 0.0;
        }
        return new double[] {distance, bearing};
    }

    /**
     * 経度を (-180, 180] にそろえる。
     * <p>
     * Normalizes longitude to (-180, 180].
     */
    static double normalizeLon(double lon) {
        double r = lon % 360.0;
        if (r > 180.0) {
            r -= 360.0;
        } else if (r <= -180.0) {
            r += 360.0;
        }
        return r;
    }
}
