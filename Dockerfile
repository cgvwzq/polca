FROM debian

# install dependencies
USER root
RUN echo 'deb http://deb.debian.org/debian/ sid main' >> /etc/apt/sources.list
RUN apt-get update && apt-get -y install \
	openjdk-8-jdk \
	maven \
	build-essential \
	autoconf \
	libtool \
	flex \
	bison \
	wget \
	graphviz

# config system
RUN useradd -U -m -u 1001 -s /bin/bash user
ADD . /home/user/polca/
RUN chown -R user:user /home/user/
RUN update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

# install sketch
WORKDIR /home/user/
USER user
RUN wget 'http://people.csail.mit.edu/asolar/sketch-1.7.5.tar.gz'
RUN tar xvfz sketch-1.7.5.tar.gz
WORKDIR sketch-1.7.5/sketch-backend/
RUN chmod +x configure && bash autogen.sh && ./configure && make -j
ENV PATH="/home/user/sketch-1.7.5/sketch-frontend:${PATH}"

# install polca
WORKDIR /home/user/polca
USER user
RUN mvn install
