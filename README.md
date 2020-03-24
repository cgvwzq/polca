# Polca

Use LearnLib to learn automata models for cache replacement policies.

Support for several simulated replacement policies.

Binding with real hardware relies on CacheQuery. (add link)

## Install

`mvn install`

We use LearnLib 0.14.0: https://learnlib.github.io/learnlib/maven-site/0.14.0/apidocs/

## Run

Help menu:

```
 usage: Polca
 -b,--binary <arg>        path to proxy for 'hw' policy
 -d,--depth <arg>         max_depth for membership queries (default: 1)
 -h,--help                show this help message
 -hit_ratio <arg>         ratio of hits to consider a HIT (default: 0.8)
 -l,--learner <arg>       learning algorithm lstar|kv|mp|rs|dhc|dt|ttt
                          (default: 'kv')
 -m,--max_size <arg>      maximum number of states of SUL
 -miss_ratio <arg>        ratio of misses to consider a MISS (default:
                          0.2)
 -no_cache                don't use cache for membership queries
 -o,--output <arg>        write learnt .dot model into output file
 -p,--policy <arg>        simulator cache policy:
                          fifo|lru|plru|lip|plip|mru|srriphp|srripfp|s
                          kyl2|skyl3|hw (default: 'fifo')
 -prefix <arg>            prefix before every query, used to fill cache
                          (default: '@')
 -r,--repetitions <arg>   number of measurements by cachequery (default:
                          100)
 -r_bound <arg>           bound on queries for equivalence, set to 0 for
                          unbounded (default: 1000)
 -r_len <arg>             expected length of random word (r_min + r_len)
                          (default: 30)
 -r_min <arg>             minimal length of random word (default: 10)
 -r_rand <arg>            TODO: select custom random generator
 -random                  use random wp-method as equivalence query
 -s,--silent              remove stdout info
 -temp                    write partial model into '.model.tmp' file
 -verbose                 output verbose information
 -votes <arg>             number of votes for deciding result (default: 1)
 -w,--ways <arg>          cache associativity (default: 4)
 ```

Example for learning LRU assoc=4 from simulator:

`./polca.sh -w 4 -p lru -verbose`

Example for learning L1 in Haswell machine:

`./polca.sh -w 8 -p hw -b \\\"ssh -t pepe@haswell ~/cachequery/cachequery.py -c ~/cachequery/cachequery.ini -i -l l1\\\" -prefix \\\"@ @\\\" -verbose`

## Docker

To build the project and all dependencies in a docker container run:

```
docker build -t polca .
```

Make `tmp/` directory writeable (i.e. `chmod o+w tmp/`) to others, and run the following to get a command line into the container:

```
sudo docker run -ti -v $(pwd)/tmp:/home/user/polca/tmp polca
```

The `tmp/` directory is mounted inside the container in order to share output files with the host.

In order to verify that everything works as expected run:

```
user@xxx:~/polca$ ./polca.sh --help
```

### Test 1 - Learn PLRU assoc=4 from simulator

```
$ ./polca.sh -w 4 -p plru -o tmp/plru.dot

```

In order to visualize the file from the host machine, run:

```
$ dot -Tpng tmp/plru.dot | feh -
```

Or, if `dot` or `feh` are not installed in the host system, convert the dot file into a PNG image inside the container:

```
dot -Tpng tmp/plru.dot -o tmp/plru.png
```

And open it with any tool of your choice.

Feel free to modify the associativty or the policy under learning. All the learned policies from a simulator are available at `models/simul/`.

### Test 2 - Synthesize exaplanation for LRU

First of all we need to convert the automata model into a Sketch file, for this we run:

```
$ bash scripts/dot_to_constraints.sh models/simul/lru_4.dot 4 | tee tmp/lru4.sk
```

Compare (e.g. via `diff`) the resulting file with `~/polca/scripts/sketch/lru4.sk`, and run:

```
$ cd scripts/sketch/
$ sketch lru4.sk | tee ../../tmp/lru.out
```

The resulting file is an explanation for the LRU policy.

Directory `scripts/sketch/` contains all the Sketch files with the corresponding automata constraints; `scripts/sketch/output/` contains all the explanations/solutions (as `*.out` files) and a manually cleaned version of them (as `*.clean` files).




## Hardware Examples

| CPU | Level | Ways | States | Description | Link to model | Prefix | HW mem queries | HW eq queries |
|--- |--- |--- |---	|--- |--- |--- |--- |--- |
| Haswell i7-4790 | L1 | 8 | 128 | Tree-PLRU | [dot](models/haswell_l1.dot) | @ @ | 3584 | 36158 |
| Haswell i7-4790 | L2 | 8 | 128 | Tree-PLRU | [dot](models/haswell_l2.dot) | @ | 3584 | 36158 |
| Skylake i5-6500 | L1 | 8 | 128 | Tree-PLRU | [dot](models/skylake_l1.dot) | @ | 3584 | 36158 |
| Skylake i5-6500 | L2 | 4 | 160 | QLRU variant | [dot](models/skylake_l2.dot) | d c b a @ | 3525 | 42225 |
| Skylake i5-6500 | L3 | 4 (with CAT) | 175 | QLRU variant | [dot](models/skylake_l3-w4.dot) | @ | 3804 | 45117 |
| KabyLake R i7-8550U | L1 | 8 | 128 | Tree-PLRU | [dot](models/kabylake_l1.dot) | @ | 3584 | 36158 |
| KabyLake R i7-8550U | L2 | 4 | 160 | QLRU variant | [dot](#missing) | d c b a @ | 3525 | 42225 |
| KabyLake R i7-8550U | L3 | 4 (with CAT) | 175 | QLRU variant | [dot](models/kabylake_l3-w4.dot) | @ | 3804 | 45117 |

We include the learnt models (.dot files) under the `models/` directory.

Prefixes are used to fill the cache with initial content and put it to the same control state.

## Simulator Examples

We also include learnt models for all the policies supported by simulator (see `models/simul/`), with associativity 4:

| Policy | States (assoc=n) | States (assoc=4) | Link to model | Learning time |
|--- |--- |--- |--- |--- |
| FIFO | n | 4 | [dot](models/simul/fifo_4.dot) | 0.081s |
| LRU | n! | 24 | [dot](models/simul/lru_4.dot) | 0.307s |
| PLRU | 2^(n-1) | 8 | [dot](models/simul/plru_4.dot) | 0.155s |
| LIP | n! | 24 | [dot](models/simul/lip_4.dot) | 0.275s |
| MRU | 2^n - 2 | 14 | [dot](models/simul/mru_4.dot) | 0.179s |
| SRRIPFP-4 | 4^n | 256 | [dot](models/simul/srripfp_4.dot) | 7.618s |
| SRRIPHP-4 | ? | 178 | [dot](models/simul/srriphp_4.dot) | 2.097s |

## Issues

* No support for TTY: sudo's prompt doesn't work trough Polca, need to use root or `NOPASSWD` in /etc/sudoers.
* ...
