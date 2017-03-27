time-jdk-executor
=======
    
My pet project.  
This is a prototype of multithreaded server executes incoming tasks on a scheduled time. Powered by: `Java SE `only.
  
  
## Rules:  

  * The server accepts tasks with `LocalDateTime` and `Callable<?>`. 
  * `LocalDateTime` is a scheduled time and `Callable<?>` is a task for execution on that time.
  * Order of the execution uses scheduled time or number of inbound order.
  * Tasks could comes in a random order and from multiple threads
  * 'Hot' tasks should not waste time and executes immediately.

  
## Requirements:

  * Java SE Development Kit 8 (or newer)  
  * Gradle 2.x (or you could use Gradle wrapper)  


## Project configuration:  

  * Java SE should be installed and you need to set path variable for `JAVA_HOME`.
  * Gradle doesn't need to install because you might do this automatically thanks Gradle Wrapper.


## Run

  * The server could be called from `JUnit` tests
