"""Step 6 - the same SpaceService from Python: no JVM, no GigaSpaces jars, only the .proto files.

Generate the stubs once into ./generated (see the README), then:  python space_client.py [host:port] [space]
Run BasicCrud first, so the Product type is registered.
"""
import sys

import grpc

sys.path.insert(0, "generated")
from space import common_pb2, space_crud_pb2, space_admin_pb2, space_service_pb2_grpc  # noqa: E402

target = sys.argv[1] if len(sys.argv) > 1 else "localhost:9091"
space = sys.argv[2] if len(sys.argv) > 2 else "grpcDemo"


def value(v):
    """A Python value as a gRPC Value. Check bool before int: in Python, True is an int too."""
    if isinstance(v, bool):
        return common_pb2.Value(bool_value=v)
    if isinstance(v, int):
        return common_pb2.Value(int64_value=v)
    if isinstance(v, float):
        return common_pb2.Value(double_value=v)
    return common_pb2.Value(string_value=str(v))


def to_python(v):
    kind = v.WhichOneof("kind")
    return getattr(v, kind) if kind else None


def as_dict(doc):
    return {name: to_python(v) for name, v in sorted(doc.properties.items())}


with grpc.insecure_channel(target) as channel:
    stub = space_service_pb2_grpc.SpaceServiceStub(channel)

    doc = common_pb2.SpaceDocument(type_name="Product", properties={
        "productId": value("py1"), "name": value("From Python"), "category": value("books"),
        "price": value(7.5), "stock": value(3)})
    stub.Write(space_crud_pb2.WriteRequest(space_name=space, document=doc), timeout=5)
    print("wrote py1")

    r = stub.ReadById(space_crud_pb2.ReadByIdRequest(space_name=space, type_name="Product",
                                                     id=value("py1")), timeout=5)
    print("readById py1 ->", as_dict(r.document) if r.HasField("document") else "no document")

    q = stub.QueryExpression(space_admin_pb2.QueryExpressionRequest(
        space_name=space, type_name="Product", expression="productId LIKE 'py%'",
        max_entries=100), timeout=5)
    print("queryExpression productId LIKE 'py%' ->",
          sorted(as_dict(d)["productId"] for d in q.documents))

    try:
        stub.ReadById(space_crud_pb2.ReadByIdRequest(space_name="noSuchSpace", type_name="Product",
                                                     id=value("py1")), timeout=5)
    except grpc.RpcError as e:
        print("readById in noSuchSpace ->", e.code().name, "-", e.details())
