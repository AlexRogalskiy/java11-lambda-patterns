# java11-lambda-patterns
Functional programming patterns in java.

_Reference_: https://www.youtube.com/watch?v=YnzisJh-ZNI  
_Reference_: https://www.youtube.com/watch?v=e4MT_OguDKg&index=146  
_Reference_: https://www.amazon.com/Modern-Java-Action-functional-programming/dp/1617293563

# introduction
This repo is a mix of functional design patterns that we have seen
in books or on the internet. 

# project description
1.  try to design you API in a composable way (package: **composable**)
    ```
    class ShoppingAPI {
        static Function<List<Item>, Cart> buy() {
            return Cart::new;
        }
    
        static Function<Cart, Order> order() {
            return Order::new;
        }
    
        static Function<Order, Delivery> deliver() {
            return Delivery::new;
        }
    
        static Function<List<Item>, Delivery> oneClickBuy() {
            return buy()
                    .andThen(order())
                    .andThen(deliver());
        }
    }
    ```
    * other used classes are as simple as they can be:
        ```
        @Value
        class Cart {
            ImmutableList<Item> items;
        
            Cart(List<Item> items) {
                this.items = ImmutableList.copyOf(items);
            }
        }
        
        @Value
        class Delivery {
            Order order;
        }
        
        @Value
        class Item {
            int id;
        }
        
        @Value
        class Order {
            Cart cart;
        }
        ```
1. it's often helpful to use currying(https://github.com/mtumilowicz/groovy-closure-currying) 
and functional interfaces to design API
    ```
    @FunctionalInterface
    interface CurrableDoubleBinaryOperator extends DoubleBinaryOperator {
    
        default DoubleUnaryOperator rate(double u) {
            return t -> applyAsDouble(t, u);
        }
    }
    ```
    then we can easily implement conversion classes
    ```
    class RateConverter implements CurrableDoubleBinaryOperator {
    
        @Override
        public double applyAsDouble(double value, double rate) {
            return value * rate;
        }
    
        static DoubleUnaryOperator milesToKmConverter() {
            return new RateConverter().rate(1.609);
        }
    
        static DoubleUnaryOperator celsiusToFahrenheitConverter() {
            return new RateConverter().rate(1.8).andThen(x -> x + 32);
        }
    }
    ```
1. use tuples and know the stream API
    ```
    @Value
    @Builder
    public class Customer {
        ImmutableList<Order> orders;
        ImmutableList<Expense> expenses;
        
        // ... methods
    }
    
    @Value
    @Builder
    class Expense {
        Year year;
        ImmutableSet<String> tags;
    
        Stream<String> getTagsStream() {
            return SetUtils.emptyIfNull(tags).stream();
        }
    }
    ```
    and we want to:
    * find order with max price
        ```
        Optional<Order> findOrderWithMaxPrice() {
            return ListUtils.emptyIfNull(orders).stream()
                    .filter(Order::hasPrice)
                    .max(comparing(Order::getPrice));
        
        }
        ```
    * find top3 orders by price
        ```
        Triple<Order, Order, Order> findTop3OrdersByPrice() {
            return ListUtils.emptyIfNull(orders).stream()
                    .filter(Order::hasPrice)
                    .sorted(comparing(Order::getPrice, reverseOrder()))
                    .limit(3)
                    .collect(collectingAndThen(toList(), ListToTripleConverter::convert));
        }
        ```
    * construct an immutable map with (key, value) = (year, tags from that year)
        ```
        ImmutableMap<Year, Set<String>> yearTagsExpensesMap() {
            return ListUtils.emptyIfNull(expenses).stream()
                    .collect(collectingAndThen(groupingBy(Expense::getYear, flatMapping(Expense::getTagsStream, toSet())),
                            ImmutableMap::copyOf)
                    );
        }
        ```
1. try to avoid decorator pattern - use function composition instead
    ```
    @Value
    @RequiredArgsConstructor
    class Camera {
        Function<Color, Color> transformColors;
    
        Camera() {
            this.transformColors = Function.identity();
        }
    
        Camera withFilter(Function<Color, Color> transform) {
            return new Camera(transformColors.andThen(transform));
        }
    
        Color snap(Color color) {
            return transformColors.apply(color);
        }
    }
    ```
    and a library of functions to transform colors
    ```
    class ColorTransformers {
        static Color brighten(Color color, int modifier) {
            Preconditions.checkArgument(nonNull(color));
            Preconditions.checkArgument(modifier >= 0);
    
            return new Color(red(color) + modifier,
                    green(color) + modifier,
                    blue(color) + modifier);
        }
    
        static Color negate(Color color) {
            Preconditions.checkArgument(nonNull(color));
    
            return new Color(negate(red(color)), negate(green(color)), negate(blue(color)));
        }
    
        private static int negate(int color) {
            Preconditions.checkArgument(color <= 255);
            Preconditions.checkArgument(color >= 0);
    
            return 255 - color;
        }
    
        private static int red(Color color) {
            return color.getRed();
        }
    
        private static int green(Color color) {
            return color.getGreen();
        }
    
        private static int blue(Color color) {
            return color.getBlue();
        }
    }
    ```
1. create complex DSL with hiding creation inside
    ```
    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public class Mailer {
        private static final Mailer EMPTY = new Mailer();
    
        String from;
        String to;
    
        private Mailer() {
            this.from = "";
            this.to = "";
        }
    
        Mailer from(String from) {
            return new Mailer(StringUtils.defaultIfEmpty(from, ""), to);
        }
    
        Mailer to(String to) {
            return new Mailer(from, StringUtils.defaultIfEmpty(to, ""));
        }
    
        static void send(UnaryOperator<Mailer> block) {
            System.out.println(block.apply(EMPTY));
        }
    }
    ```
    and the example of usage:
    ```
    Mailer.send(
        mailer -> mailer.from("mtumilowicz01@gmail.com")
                        .to("abc@o2.pl")
    )
    ```
    **note that in any point we don't have direct access to the object,
    we cannot create object manually and we cannot reuse it
    (there is NO Mailer object)**