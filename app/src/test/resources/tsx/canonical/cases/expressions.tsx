"ui";

const items = compose.state<string[]>(['a', 'b']);

const Increment = compose.Button;
const base = compose.modifier();

compose.mount(function () {
    const visible = items.value.length > 0;
    return (
        <compose.Column>
            {visible && <compose.Text text='Visible' />}
            {items.value.map(item => <compose.Text key={item} text={item} />)}
            {/* A comment child stays where it is. */}
            {...items.value}
            <Increment onClick={() => items.value = []}>Clear</Increment>
            <compose.Button onClick={<T,>(value: T) => value}>Generic</compose.Button>
            <compose.Button onClick={async () => { await Promise.resolve(); }}>Async</compose.Button>
            <compose.Button onClick={function named() { return 1; }}>Named</compose.Button>
            <compose.Button onClick={(event: unknown): void => {}}>Typed return</compose.Button>
            <compose.Button onLongClick={(page: number) => {
                items.value = items.value.slice(page);
                console.log(page as number);
            }}>Typed</compose.Button>
            <compose.Text text='Braced element' />
        </compose.Column>
    );
});
