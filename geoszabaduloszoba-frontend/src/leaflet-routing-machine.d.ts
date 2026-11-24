declare module 'leaflet-routing-machine' {
	export type RoutingControl = import('leaflet').Control & {
		setWaypoints(waypoints: import('leaflet').LatLng[]): void;
	};

	export interface RoutingMachine {
		Routing: {
			control(options: {
				waypoints: import('leaflet').LatLng[];
				lineOptions: { styles: import('leaflet').PathOptions[] };
				addWaypoints: boolean;
				draggableWaypoints: boolean;
				show: boolean;
				createMarker: () => null;
			}): RoutingControl;
		};
	}
}
