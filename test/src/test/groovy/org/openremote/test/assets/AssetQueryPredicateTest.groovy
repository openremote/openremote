package org.openremote.test.assets

import com.vividsolutions.jts.geom.Coordinate
import org.openremote.manager.rules.AssetQueryPredicate
import org.openremote.model.query.filter.GeofencePredicate
import org.openremote.model.query.filter.RadialGeofencePredicate
import org.openremote.model.query.filter.RectangularGeofencePredicate
import spock.lang.Specification

import java.util.function.Predicate
import java.util.function.Supplier

class AssetQueryPredicateTest extends Specification {

    def "Rectangular Geofence Test"() {
        given:
        GeofencePredicate geofencePredicate = new RectangularGeofencePredicate(51.440914, 5.421723, 51.442755, 5.425151)
        def coordinatePredicate = geofencePredicate.asPredicate({ -> System.currentTimeMillis() })
        def coordinatePredicateNegated = coordinatePredicate.negate()

        expect:
        coordinatePredicate.test(new Coordinate(5.423, 51.441))
        !coordinatePredicateNegated.test(new Coordinate(5.423, 51.441))
    }

    def "Radial geofence test"() {
        given:
        GeofencePredicate geofencePredicate = new RadialGeofencePredicate(100, 51.423, 5.441)
        GeofencePredicate geofencePredicateNegated = new RadialGeofencePredicate(100, 51.423, 5.441).negate()
        def coordinatePredicate = geofencePredicate.asPredicate({ -> System.currentTimeMillis() })
        def coordinatePredicateNegated = coordinatePredicate.negate()

        expect:
        coordinatePredicate.test(new Coordinate(5.441, 51.423))
        !coordinatePredicateNegated.test(new Coordinate(5.441, 51.423))
    }
}
