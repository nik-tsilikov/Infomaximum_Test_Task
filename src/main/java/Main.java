import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {

    private static HashMap<Item, Integer> itemMap = new HashMap<>();
    private static TreeMap<String, TreeMap<Integer, Integer>> floorCounterMap = new TreeMap<String, TreeMap<Integer, Integer>>();

    public static void main(String args[]){
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader parser;
        try {
            parser = factory.createXMLStreamReader(new FileInputStream(args[0]));
        } catch (FileNotFoundException ex) {
            System.out.println("File reading error. Please, check the path to file and restart application");
            return;
        } catch (XMLStreamException ex) {
            System.out.println(ex.getMessage());
            return;
        }

        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService threadPool = Executors.newFixedThreadPool(cores);
        System.out.println("Processing. Please, wait...");
        // Построчный парсинг файла со сбором статистики
        try {
            while (parser.hasNext()) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (parser.getLocalName() == "item") {
                        String city = parser.getAttributeValue(0);
                        String street = parser.getAttributeValue(1);
                        Integer house = Integer.parseInt(parser.getAttributeValue(2));
                        Integer floor = Integer.parseInt(parser.getAttributeValue(3));
                        Item newItem = new Item(city, street, house, floor);
                        threadPool.submit(new CountingItems(newItem));
                        threadPool.submit(new CountingFloors(city, floor));
                    }
                }
            }
        } catch (XMLStreamException ex) {
            System.out.println(ex.getMessage());
            return;
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            System.out.println(ex.getMessage());
        }
        // Фильтр для перемещения в новую коллекцию записей, имеющих дубли
        Map<Item, Integer> duplicates = itemMap.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        // Вывод в консоль результатов
        // Вывод информации о дублирующихся записях
        System.out.println("I. Duplicate entries");
        String result1 = "";
        int serviceCounter = 1;
        for (Map.Entry<Item, Integer> entry : duplicates.entrySet()) {
            Item item = entry.getKey();
            Integer dupCount = entry.getValue();
            result1 = serviceCounter + ". " + item.toString() + ". Number of repetitions: " + dupCount;
            System.out.println(result1);
            serviceCounter++;
        }

        // Вывод информации об этажности зданий в городах
        System.out.println("");
        System.out.println("II. Information on the number of buildings with 1, 2, 3, 4 5 floors");
        serviceCounter = 1;
        for (Map.Entry<String, TreeMap<Integer, Integer>> entry : floorCounterMap.entrySet()) {
            System.out.println(serviceCounter + ". " + entry.getKey());
            serviceCounter++;
            for (Map.Entry<Integer, Integer> innerEntry : entry.getValue().entrySet()) {
                System.out.println(" " + innerEntry.getKey() + "-floor(s): " + innerEntry.getValue());
            }
        }
    }

    static class CountingItems implements Runnable {
        private Item newItem;
        CountingItems(Item item) {
            this.newItem = item;
        }
        @Override
        public void run() {
            // Подсчет количества появлений каждой записи
            synchronized (itemMap) {
                Integer innerRecordCounter = itemMap.get(newItem);
                if (innerRecordCounter == null) {
                    itemMap.put(newItem, 1);
                } else {
                    itemMap.put(newItem, innerRecordCounter + 1);
                }
            }
        }
    }

    static class CountingFloors implements Runnable {
        private String city;
        private int floors;
        CountingFloors(String city, int floors) {
            this.city = city;
            this.floors = floors;
        }

        @Override
        public void run() {
            synchronized (floorCounterMap) {
                TreeMap<Integer, Integer> innerFloorCounterMap = floorCounterMap.get(city);
                // Проверяем, есть ли у нас запись о городе
                // Если нет - создаем запись о городе и количестве этажей в городе
                if (innerFloorCounterMap == null) {
                    innerFloorCounterMap = new TreeMap<Integer, Integer>();
                    innerFloorCounterMap.put(floors, 1);
                    floorCounterMap.put(city, innerFloorCounterMap);
                    return;
                }
                // Проверяем, есть ли у нас запись о зданиях в указананом городе с числом этажей, равным floor
                // Если нет - создаем запись о такой этажности, в ином случае просто инкрементируем значение
                Integer innerCounter = innerFloorCounterMap.get(floors);
                if (innerCounter == null) {
                    innerFloorCounterMap.put(floors, 1);
                } else {
                    innerFloorCounterMap.put(floors, innerCounter + 1);
                }
                floorCounterMap.put(city, innerFloorCounterMap);
            }
        }
    }
}

// Класс для хранения записей, считанных из XML-файла
class Item {
    private String city;
    private String street;
    private Integer house;
    private  Integer floor;

    public Item() {}

    public Item(String city, String street, Integer house, Integer floor) {
        this.city = city;
        this.street = street;
        this.house = house;
        this.floor = floor;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public Integer getHouse() {
        return house;
    }

    public void setHouse(Integer house) {
        this.house = house;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    @Override
    public String toString() {
        return "City: " + city + ", Street: " + street +
                " House: " + house + ", Floor: " + floor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        Item item = (Item) o;
        return Objects.equals(getCity(), item.getCity()) &&
                Objects.equals(getStreet(), item.getStreet()) &&
                Objects.equals(getHouse(), item.getHouse()) &&
                Objects.equals(getFloor(), item.getFloor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCity(), getStreet(), getHouse(), getFloor());
    }
}






