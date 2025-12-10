package fdse.microservice.service;

import edu.fudan.common.util.Response;
import fdse.microservice.entity.*;
import fdse.microservice.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Random;

import javax.validation.constraints.Null;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;


@Service
public class StationServiceImpl implements StationService {

    @Autowired
    private StationRepository repository;

    String success = "Success";

    private static final Logger LOGGER = LoggerFactory.getLogger(StationServiceImpl.class);

    @Override
    public Response create(Station station, HttpHeaders headers) {
        if(station.getName().isEmpty()) {
            StationServiceImpl.LOGGER.error("[create][Create station error][Name not specify]");
            return new Response<>(0, "Name not specify", station);
        }
        if (repository.findByName(station.getName()) == null) {
            station.setStayTime(station.getStayTime());
            Station newStation = repository.save(station);
            return new Response<>(1, "Create success", newStation);
        }
        StationServiceImpl.LOGGER.error("[create][Create station error][Already exists][StationId: {}]",station.getId());
        return new Response<>(0, "Already exists", station);
    }


    @Override
    public boolean exist(String stationName, HttpHeaders headers) {
        boolean result = false;
        if (repository.findByName(stationName) != null) {
            result = true;
        }
        return result;
    }

    @Override
    public Response update(Station info, HttpHeaders headers) {

        Optional<Station> op = repository.findById(info.getId());
        if (!op.isPresent()) {
            StationServiceImpl.LOGGER.error("[update][Update station error][Station not found][StationId: {}]",info.getId());
            return new Response<>(0, "Station not exist", null);
        } else {
            Station station = op.get();
            station.setName(info.getName());
            station.setStayTime(info.getStayTime());
            repository.save(station);
            return new Response<>(1, "Update success", station);
        }
    }

    @Override
    public Response delete(String stationsId, HttpHeaders headers) {
        Optional<Station> op = repository.findById(stationsId);
        if (op.isPresent()) {
            Station station = op.get();
            repository.delete(station);
            return new Response<>(1, "Delete success", station);
        }
        StationServiceImpl.LOGGER.error("[delete][Delete station error][Station not found][StationId: {}]",stationsId);
        return new Response<>(0, "Station not exist", null);
    }

    @Override
    public Response query(HttpHeaders headers) {
        List<Station> stations = repository.findAll();
        if (stations != null && !stations.isEmpty()) {
            return new Response<>(1, "Find all content", stations);
        } else {
            StationServiceImpl.LOGGER.warn("[query][Query stations warn][Find all stations: {}]","No content");
            return new Response<>(0, "No content", null);
        }
    }

    @Override
    public Response queryForId(String stationName, HttpHeaders headers) {
        // 检查是否启用延迟故障
        if (fdse.microservice.controller.FaultController.isStationQueryDelayEnabled()) {
            try {
                StationServiceImpl.LOGGER.warn("[queryForId][Query station id][Fault injection: delay {}ms]", 
                    fdse.microservice.controller.FaultController.getDelayTime());
                Thread.sleep(fdse.microservice.controller.FaultController.getDelayTime());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                StationServiceImpl.LOGGER.error("[queryForId][Query station id][Fault injection delay interrupted]", e);
            }
        }
        
        // 检查是否启用返回空数据故障
        if (fdse.microservice.controller.FaultController.isEmptyStationQueryEnabled()) {
            StationServiceImpl.LOGGER.warn("[queryForId][Query station id][Fault injection: return empty result]");
            return new Response<>(0, "Fault injection: No content", stationName);
        }
        
        // 检查是否启用500错误故障
        if (fdse.microservice.controller.FaultController.isStationQuery500ErrorEnabled()) {
            StationServiceImpl.LOGGER.warn("[queryForId][Query station id][Fault injection: throw 500 error]");
            throw new RuntimeException("Fault injection: Internal server error");
        }
        
        // 检查是否启用随机500错误故障
        if (fdse.microservice.controller.FaultController.isStationQueryRandom500ErrorEnabled()) {
            Random random = new Random();
            if (random.nextInt(100) < 30) { // 30% 概率抛出500错误
                StationServiceImpl.LOGGER.warn("[queryForId][Query station id][Fault injection: throw random 500 error]");
                throw new RuntimeException("Fault injection: Random internal server error");
            }
        }
        
        // 检查是否启用响应结构错误故障
        if (fdse.microservice.controller.FaultController.isStationQueryResponseStructureErrorEnabled()) {
            StationServiceImpl.LOGGER.warn("[queryForId][Query station id][Fault injection: return malformed response structure]");
            // 返回结构错误的响应 - 缺少必要字段或字段类型错误
            Map<String, Object> malformedData = new HashMap<>();
            malformedData.put("error_message", "Fault injection: Malformed response structure");
            malformedData.put("invalid_field", 12345); // 应该是字符串但返回数字
            malformedData.put("missing_required_field", null); // 缺少必要字段
            return new Response<>(1, "Fault injection: Malformed structure", null);
        }
        
        Station station = repository.findByName(stationName);

        if (station  != null) {
            return new Response<>(1, success, station.getId());
        } else {
            StationServiceImpl.LOGGER.warn("[queryForId][Find station id warn][Station not found][StationName: {}]",stationName);
            return new Response<>(0, "Not exists", stationName);
        }
    }


    @Override
    public Response queryForIdBatch(List<String> nameList, HttpHeaders headers) {
        // 检查是否启用延迟故障
        if (fdse.microservice.controller.FaultController.isStationQueryDelayEnabled()) {
            try {
                StationServiceImpl.LOGGER.warn("[queryForIdBatch][Query station ids batch][Fault injection: delay {}ms]", 
                    fdse.microservice.controller.FaultController.getDelayTime());
                Thread.sleep(fdse.microservice.controller.FaultController.getDelayTime());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                StationServiceImpl.LOGGER.error("[queryForIdBatch][Query station ids batch][Fault injection delay interrupted]", e);
            }
        }
        
        // 检查是否启用返回空数据故障
        if (fdse.microservice.controller.FaultController.isEmptyStationQueryEnabled()) {
            StationServiceImpl.LOGGER.warn("[queryForIdBatch][Query station ids batch][Fault injection: return empty result]");
            return new Response<>(0, "Fault injection: No content", null);
        }
        
        // 检查是否启用500错误故障
        if (fdse.microservice.controller.FaultController.isStationQuery500ErrorEnabled()) {
            StationServiceImpl.LOGGER.warn("[queryForIdBatch][Query station ids batch][Fault injection: throw 500 error]");
            throw new RuntimeException("Fault injection: Internal server error");
        }
        
        // 检查是否启用随机500错误故障
        if (fdse.microservice.controller.FaultController.isStationQueryRandom500ErrorEnabled()) {
            Random random = new Random();
            if (random.nextInt(100) < 30) { // 30% 概率抛出500错误
                StationServiceImpl.LOGGER.warn("[queryForIdBatch][Query station ids batch][Fault injection: throw random 500 error]");
                throw new RuntimeException("Fault injection: Random internal server error");
            }
        }
        
        // 检查是否启用响应结构错误故障
        if (fdse.microservice.controller.FaultController.isStationQueryResponseStructureErrorEnabled()) {
            StationServiceImpl.LOGGER.warn("[queryForIdBatch][Query station ids batch][Fault injection: return malformed response structure]");
            // 返回结构错误的响应 - 数组结构错误或字段类型错误
            Map<String, Object> malformedData = new HashMap<>();
            malformedData.put("error_message", "Fault injection: Malformed batch response structure");
            malformedData.put("invalid_array", Arrays.asList(1, 2, 3, "invalid_string", null)); // 混合类型数组
            malformedData.put("wrong_data_type", "should_be_map"); // 应该是Map但返回字符串
            malformedData.put("null_values", null); // 整个字段为null
            return new Response<>(1, "Fault injection: Malformed batch structure", malformedData);
        }
        
        Map<String, String> result = new HashMap<>();
        List<Station> stations = repository.findByNames(nameList);
        Map<String, String> stationMap = new HashMap<>();
        for(Station s: stations) {
            stationMap.put(s.getName(), s.getId());
        }

        for(String name: nameList){
            result.put(name, stationMap.get(name));
        }

        if (!result.isEmpty()) {
            return new Response<>(1, success, result);
        } else {
            StationServiceImpl.LOGGER.warn("[queryForIdBatch][Find station ids warn][Stations not found][StationNameNumber: {}]",nameList.size());
            return new Response<>(0, "No content according to name list", null);
        }

    }

    @Override
    public Response queryById(String stationId, HttpHeaders headers) {
        Optional<Station> station = repository.findById(stationId);
        if (station.isPresent()) {
            return new Response<>(1, success, station.get().getName());
        } else {
            StationServiceImpl.LOGGER.error("[queryById][Find station name error][Station not found][StationId: {}]",stationId);
            return new Response<>(0, "No that stationId", stationId);
        }
    }

    @Override
    public Response queryByIdBatch(List<String> idList, HttpHeaders headers) {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < idList.size(); i++) {
            Optional<Station> stationOld = repository.findById(idList.get(i));
            if(stationOld.isPresent()){
                Station station=stationOld.get();
                result.add(station.getName());
            }
        }

        if (!result.isEmpty()) {
            return new Response<>(1, success, result);
        } else {
            StationServiceImpl.LOGGER.error("[queryByIdBatch][Find station names error][Stations not found][StationIdNumber: {}]",idList.size());
            return new Response<>(0, "No stationNamelist according to stationIdList", result);
        }

    }
}
