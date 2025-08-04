package streetwalker.userservice.services;

import com.google.protobuf.ProtocolStringList;
import locationservice.LocationServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import locationservice.Location;
import locationservice.Location.*;
@Slf4j
@Service
public class LocationServiceClient {

    @GrpcClient("location-service")
    private LocationServiceGrpc.LocationServiceBlockingStub locationServiceBlockingStub;

    /**
     * Обновляет геолокацию пользователя.
     *
     * @param userId ID пользователя.
     * @param lat    Широта.
     * @param lng    Долгота.
     * @throws RuntimeException Если обновление не удалось.
     */
    public void updateLocation(long userId, double lat, double lng) {
        UpdateLocationRequest request = UpdateLocationRequest.newBuilder()
                .setUserId(userId)
                .setLocation(Location.GeoPair.newBuilder().setLat(lat).setLng(lng).build())
                .build();
        log.info("Updating location for user {} to {}, {}", userId, lat, lng);
        UpdateLocationResponse response = locationServiceBlockingStub.updateLocation(request);
        if (!response.getSuccess()) {
            log.error("Failed to update location for user {}", userId);
            throw new RuntimeException("Failed to update location");
        }
    }

    /**
     * Получает текущую геолокацию пользователя.
     *
     * @param userId ID пользователя.
     * @return Объект GeoPair с широтой и долготой.
     */
    public GeoPair getLocation(long userId) {
        GetLocationRequest request = GetLocationRequest.newBuilder()
                .setUserId(userId)
                .build();

        GetLocationResponse response = locationServiceBlockingStub.getLocation(request);
        return new GeoPair(response.getLocation().getLat(), response.getLocation().getLng());
    }

    /**
     * Получает историю геолокаций пользователя за указанный период.
     *
     * @param userId    ID пользователя.
     * @param startTime Начало периода (в формате RFC3339).
     * @param endTime   Конец периода (в формате RFC3339).
     * @return Список объектов GeoPair с широтой и долготой.
     */public GeoPair[] getLocationHistory(long userId, String startTime, String endTime) {


        GetLocationHistoryRequest request = GetLocationHistoryRequest.newBuilder()
                .setUserId(userId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .build();

        log.info("Getting location history for user {} from {} to {}", userId, startTime, endTime);
        GetLocationHistoryResponse response = locationServiceBlockingStub.getLocationHistory(request);
        log.info("resp"+ response);
        ProtocolStringList timestamps = response.getTimestampsList();
        List<Location.GeoPair> locationList = response.getLocationsList();
        GeoPair[] history = new GeoPair[locationList.size()];

        for(int i = 0; i<response.getTimestampsCount(); i++){
            history[i] = new GeoPair(locationList.get(i).getLat(), locationList.get(i).getLng(), OffsetDateTime.parse(timestamps.get(i)) );
        }
        return history;
    }
    public long[] getUsersByLatLng(double lat, double lng, int users) {
        Location.GetUsersBetweenLongAndLatRequest request = Location.GetUsersBetweenLongAndLatRequest.newBuilder()

                .setLocation(
                        Location.GeoPair.newBuilder().setLat(lat).setLng(lng).build()
                )
                .setMaxUsers(users)
                .build();
        Location.GetUsersBetweenLongAndLatResponse response = locationServiceBlockingStub.getUsersBetweenLongAndLat(request);
        return response.getUserList().stream()
                .mapToLong(Location.UserResponse::getUserId)
                .toArray();
    }

}